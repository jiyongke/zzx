package com.orange.farm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orange.farm.common.BusinessException;
import com.orange.farm.dto.TraceNodeDTO;
import com.orange.farm.entity.TraceBatch;
import com.orange.farm.entity.TraceNode;
import com.orange.farm.entity.User;
import com.orange.farm.mapper.TraceBatchMapper;
import com.orange.farm.mapper.TraceNodeMapper;
import com.orange.farm.util.UserContext;
import com.orange.farm.vo.TraceBatchVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * 溯源服务（核心模块）
 *
 * 可信设计：
 * 1. 准入可信——农户须通过实名资质认证方可新建批次/商品；
 * 2. 录入可信——八大环节按业务顺序录全，质检环节由平台核验后录入；
 * 3. 存证可信——发布即对溯源链计算 SHA-256 摘要哈希入库，查询时重新计算比对，发布后记录锁定仅可追加更正；
 * 4. 查询可信——扫码计数、首次扫码提示、异常高频扫码防伪预警。
 */
@Service
public class TraceService {

    /** 八大环节业务顺序（选种→施肥→防治→采收→加工→质检→仓储→物流） */
    private static final List<String> NODE_ORDER = Arrays.asList(
            "PLANT", "FERTILIZE", "PEST", "HARVEST", "PROCESS", "QUALITY", "STORAGE", "LOGISTICS");

    private static final List<String> NODE_NAMES = Arrays.asList(
            "选种育苗", "有机施肥", "绿色防治", "人工采收", "加工分拣", "质检认证", "仓储保鲜", "冷链物流");

    /** 更正记录类型（发布后由管理员追加） */
    private static final String NODE_CORRECTION = "CORRECTION";

    /** 农户提交发布审核前须录完的环节数：选种/施肥/防治/采收/加工（质检起由管理员核验补录） */
    private static final int FARMER_SUBMIT_REQUIRED = 5;

    @Resource
    private TraceBatchMapper traceBatchMapper;

    @Resource
    private TraceNodeMapper traceNodeMapper;

    @Resource
    private AuditService auditService;

    // ---------------- 消费者查询 ----------------

    /**
     * 可溯源批次列表（公开）
     */
    public List<TraceBatch> publicList() {
        return traceBatchMapper.selectList(new QueryWrapper<TraceBatch>()
                .eq("status", 1)
                .orderByDesc("id"));
    }

    /**
     * 批次详情（含环节链，公开；不触发扫码计数）
     */
    public TraceBatchVO detail(Long id) {
        TraceBatch batch = traceBatchMapper.selectById(id);
        if (batch == null) {
            throw new BusinessException("溯源批次不存在");
        }
        return buildVO(batch);
    }

    /**
     * 二维码/批次号查询（公开，核心溯源入口）
     * 查询时：① 重算哈希比对校验数据是否被篡改；② 扫码计数并做首次/高频防伪提示
     */
    public TraceBatchVO queryByCode(String code) {
        TraceBatch batch = traceBatchMapper.selectOne(new QueryWrapper<TraceBatch>()
                .eq("batch_code", code.trim()));
        if (batch == null) {
            throw new BusinessException("未查询到该溯源批次，请核对批次号或二维码");
        }
        if (batch.getStatus() == null || batch.getStatus() != 1) {
            throw new BusinessException("该溯源批次已下架或未发布，暂不可查询");
        }

        Date now = new Date();
        int prevScan = batch.getScanCount() == null ? 0 : batch.getScanCount();
        Date prevLast = batch.getLastScanTime();
        // 高频防伪预警：短时间内被反复扫描（疑似复制二维码批量核验）
        boolean highFreq = prevScan > 0 && prevLast != null
                && (now.getTime() - prevLast.getTime()) < 2000;

        TraceBatch update = new TraceBatch();
        update.setId(batch.getId());
        update.setScanCount(prevScan + 1);
        update.setLastScanTime(now);
        traceBatchMapper.updateById(update);
        batch.setScanCount(prevScan + 1);
        batch.setLastScanTime(now);

        TraceBatchVO vo = buildVO(batch);
        vo.setFirstScan(prevScan == 0);
        vo.setHighFreqWarning(highFreq);
        return vo;
    }

    // ---------------- 农户/管理员管理 ----------------

    /**
     * 批次管理分页（农户看自己的，管理员看全部）
     */
    public Page<TraceBatch> batchPage(int page, int size, String keyword) {
        QueryWrapper<TraceBatch> wrapper = new QueryWrapper<>();
        if (!"ADMIN".equals(UserContext.getRole())) {
            wrapper.eq("farmer_id", UserContext.getUserId());
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("product_name", keyword).or().like("batch_code", keyword));
        }
        wrapper.orderByDesc("id");
        return traceBatchMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public TraceBatchVO batchDetail(Long id) {
        TraceBatch batch = checkBatchOwner(id);
        return buildVO(batch);
    }

    public void createBatch(TraceBatch batch) {
        User user = UserContext.get();
        // 资质门槛仅约束农户（准入可信）；管理员可代建/监管批次
        if ("FARMER".equals(user.getRole())) {
            checkFarmerQualified(user);
        }
        batch.setId(null);
        batch.setFarmerId(user.getId());
        batch.setFarmerName(user.getNickname());
        if (batch.getBatchCode() == null || batch.getBatchCode().isEmpty()) {
            batch.setBatchCode(generateBatchCode());
        }
        if (batch.getStatus() == null) {
            batch.setStatus(0);
        }
        traceBatchMapper.insert(batch);
        auditService.log("CREATE", "TRACE_BATCH", batch.getId(),
                "新建溯源批次: " + batch.getBatchCode() + " " + batch.getProductName());
    }

    public void updateBatch(TraceBatch batch) {
        TraceBatch old = checkBatchOwner(batch.getId());
        // 发布/待审核后批次信息锁定，仅可追加更正记录
        if (old.getStatus() != null && old.getStatus() != 0) {
            throw new BusinessException("批次已提交审核或已发布，信息已锁定，仅可追加更正记录");
        }
        if ("FARMER".equals(UserContext.getRole())) {
            checkFarmerQualified(UserContext.get());
        }
        batch.setBatchCode(old.getBatchCode());
        traceBatchMapper.updateById(batch);
        auditService.log("UPDATE", "TRACE_BATCH", batch.getId(),
                "修改溯源批次: " + batch.getBatchCode());
    }

    /**
     * 提交审核（农户：0→3）或直接发布（管理员：0→1，跳过审核并计算哈希）
     */
    public void publish(Long id) {
        TraceBatch batch = checkBatchOwner(id);
        if (batch.getStatus() == null || batch.getStatus() != 0) {
            throw new BusinessException("仅待完善批次可提交发布");
        }
        if ("ADMIN".equals(UserContext.getRole())) {
            // 管理员直发：八大环节须全部录齐
            checkChainComplete(batch);
            doPublish(batch, "管理员直接发布溯源链: ");
        } else {
            // 农户：资质 + 前5环节（到加工）录齐即可提交，质检/仓储/物流由管理员审核时补录
            checkFarmerQualified(UserContext.get());
            checkFarmerSubmitComplete(batch);
            batch.setStatus(3);
            traceBatchMapper.updateById(batch);
            auditService.log("SUBMIT_REVIEW", "TRACE_BATCH", id,
                    "提交溯源链发布审核: " + batch.getBatchCode() + " " + batch.getProductName());
        }
    }

    /**
     * 管理员审核溯源链发布申请：通过（3→1，计算哈希存证）/ 驳回（3→0）
     */
    public void review(Long id, boolean approve, String remark) {
        TraceBatch batch = traceBatchMapper.selectById(id);
        if (batch == null) {
            throw new BusinessException("溯源批次不存在");
        }
        if (batch.getStatus() == null || batch.getStatus() != 3) {
            throw new BusinessException("该批次不在待审核状态");
        }
        if (approve) {
            checkChainComplete(batch);
            doPublish(batch, "审核通过并发布溯源链: ");
        } else {
            batch.setStatus(0);
            traceBatchMapper.updateById(batch);
            auditService.log("REJECT", "TRACE_BATCH", id,
                    "驳回溯源链发布申请: " + batch.getBatchCode()
                            + (remark == null || remark.isEmpty() ? "" : "（" + remark.trim() + "）"));
        }
    }

    /** 发布：状态置为可溯源并计算溯源链哈希存证 */
    private void doPublish(TraceBatch batch, String auditPrefix) {
        batch.setStatus(1);
        batch.setHashValue(computeChainHash(batch, nodesOf(batch.getId())));
        traceBatchMapper.updateById(batch);
        auditService.log("PUBLISH", "TRACE_BATCH", batch.getId(),
                auditPrefix + batch.getBatchCode() + " " + batch.getProductName()
                        + "（哈希: " + shortHash(batch.getHashValue()) + "）");
    }

    /**
     * 下架批次（状态 1 → 2）
     */
    public void offline(Long id) {
        TraceBatch batch = checkBatchOwner(id);
        batch.setStatus(2);
        traceBatchMapper.updateById(batch);
        auditService.log("OFFLINE", "TRACE_BATCH", id,
                "下架溯源链: " + batch.getBatchCode());
    }

    public void deleteBatch(Long id) {
        TraceBatch batch = checkBatchOwner(id);
        if ("FARMER".equals(UserContext.getRole()) && batch.getStatus() != null && batch.getStatus() == 1) {
            throw new BusinessException("已发布批次不可删除，请联系平台管理员处理");
        }
        traceNodeMapper.delete(new QueryWrapper<TraceNode>().eq("batch_id", id));
        traceBatchMapper.deleteById(id);
        auditService.log("DELETE", "TRACE_BATCH", id,
                "删除溯源批次: " + batch.getBatchCode());
    }

    /**
     * 录入溯源环节
     * - 待完善（status=0）状态下可录入；八大环节按业务顺序逐条录入；
     * - 质检环节（QUALITY）须由平台管理员核验后录入，农户不可自行录入；
     * - 批次发布/待审核后记录锁定，仅管理员可追加「更正记录」（CORRECTION）。
     */
    public void addNode(TraceNodeDTO dto) {
        if (dto.getBatchId() == null) {
            throw new BusinessException("批次ID不能为空");
        }
        TraceBatch batch = traceBatchMapper.selectById(dto.getBatchId());
        if (batch == null) {
            throw new BusinessException("溯源批次不存在");
        }
        String role = UserContext.getRole();
        checkBatchOwner(batch.getId());

        Integer st = batch.getStatus();
        boolean published = st != null && st != 0;
        // 待审核（status=3）时，管理员按顺序补录质检/仓储/物流环节（农户仍锁定）
        boolean reviewFill = published && st == 3 && "ADMIN".equals(role)
                && !NODE_CORRECTION.equals(dto.getNodeType());
        if (published && !reviewFill) {
            // 发布/待审核后锁定：仅管理员可在已发布批次追加更正记录
            if (!"ADMIN".equals(role) || !NODE_CORRECTION.equals(dto.getNodeType())) {
                throw new BusinessException("批次已提交审核或已发布，溯源记录已锁定，仅平台管理员可追加更正记录");
            }
        }
        if (!published) {
            if ("FARMER".equals(role)) {
                checkFarmerQualified(UserContext.get());
            }
            if (NODE_CORRECTION.equals(dto.getNodeType())) {
                throw new BusinessException("更正记录仅可在批次发布后由管理员追加");
            }
        }

        // 质检环节：平台核验后录入（避免单一主体自证）
        if ("QUALITY".equals(dto.getNodeType()) && !"ADMIN".equals(role)) {
            throw new BusinessException("质检环节须由平台管理员核验后录入");
        }

        List<TraceNode> nodes = nodesOf(batch.getId());
        int nextSort = nodes.size() + 1;
        boolean correction = NODE_CORRECTION.equals(dto.getNodeType());
        // 普通环节按业务顺序校验（更正记录不占八环节顺序）
        if (!correction) {
            int idx = countNormalNodes(nodes);
            if (idx >= NODE_ORDER.size()) {
                throw new BusinessException("八大环节已录全，请提交审核发布溯源链");
            }
            String expected = NODE_ORDER.get(idx);
            if (!expected.equals(dto.getNodeType())) {
                throw new BusinessException("请按业务顺序录入溯源环节，下一步应录入「" + NODE_NAMES.get(idx) + "」");
            }
        }

        TraceNode node = new TraceNode();
        node.setBatchId(dto.getBatchId());
        node.setNodeType(dto.getNodeType());
        node.setTitle(dto.getTitle());
        node.setDescription(dto.getDescription());
        node.setImages(dto.getImages());
        node.setOperator(dto.getOperator() == null || dto.getOperator().isEmpty()
                ? UserContext.get().getNickname() : dto.getOperator());
        node.setLocation(dto.getLocation());
        node.setRecordTime(dto.getRecordTime() == null ? new Date() : dto.getRecordTime());
        node.setSort(nextSort);
        // 存证留痕：每条记录携带录入人、角色与录入时间
        User recorder = UserContext.get();
        node.setRecorderId(recorder.getId());
        node.setRecorderRole(recorder.getRole());
        node.setRecorderTime(new Date());
        traceNodeMapper.insert(node);
        auditService.log("NODE_ADD", "TRACE_NODE", node.getId(),
                (published ? "追加更正记录: " : "录入溯源环节: ") + dto.getNodeType()
                        + " - " + dto.getTitle() + "（批次 " + batch.getBatchCode() + "）");
        // 合法追加更正记录后，同步重算并更新存证哈希（更正本身经授权且全程留痕，防伪仍有效）
        if (correction && st != null && st == 1) {
            List<TraceNode> all = nodesOf(batch.getId());
            TraceBatch hashUp = new TraceBatch();
            hashUp.setId(batch.getId());
            hashUp.setHashValue(computeChainHash(batch, all));
            traceBatchMapper.updateById(hashUp);
        }
    }

    public void deleteNode(Long nodeId) {
        TraceNode node = traceNodeMapper.selectById(nodeId);
        if (node == null) {
            throw new BusinessException("环节不存在");
        }
        TraceBatch batch = checkBatchOwner(node.getBatchId());
        if (batch.getStatus() != null && batch.getStatus() != 0) {
            throw new BusinessException("批次已提交审核或已发布，溯源记录已锁定，不可删除");
        }
        traceNodeMapper.deleteById(nodeId);
    }

    // ---------------- 内部方法 ----------------

    private void checkFarmerQualified(User user) {
        if (!UserService.isQualified(user)) {
            throw new BusinessException("您尚未通过实名与资质认证，请先在个人中心提交资质认证并经管理员审核通过后再操作");
        }
    }

    /** 农户提交审核前校验：前5环节（选种→加工）已按顺序录齐 */
    private void checkFarmerSubmitComplete(TraceBatch batch) {
        List<TraceNode> nodes = nodesOf(batch.getId());
        int done = countNormalNodes(nodes);
        if (done < FARMER_SUBMIT_REQUIRED) {
            throw new BusinessException("请先录完选种、施肥、防治、采收、加工环节（"
                    + done + "/5），下一步应录入「" + NODE_NAMES.get(done) + "」");
        }
        for (int i = 0; i < FARMER_SUBMIT_REQUIRED; i++) {
            if (!NODE_ORDER.get(i).equals(nodes.get(i).getNodeType())) {
                throw new BusinessException("溯源环节顺序不正确：第" + (i + 1) + "个环节应为「" + NODE_NAMES.get(i) + "」");
            }
        }
    }

    /** 校验八大环节是否已按顺序录全 */
    private void checkChainComplete(TraceBatch batch) {
        List<TraceNode> nodes = nodesOf(batch.getId());
        if (nodes.size() < NODE_ORDER.size()) {
            int idx = nodes.size();
            throw new BusinessException("溯源环节未录全（" + nodes.size() + "/8），下一步应录入「"
                    + NODE_NAMES.get(idx) + "」，录全后方可发布");
        }
        for (int i = 0; i < NODE_ORDER.size(); i++) {
            if (!NODE_ORDER.get(i).equals(nodes.get(i).getNodeType())) {
                throw new BusinessException("溯源环节顺序不正确：第" + (i + 1) + "个环节应为「"
                        + NODE_NAMES.get(i) + "」");
            }
        }
    }

    private TraceBatch checkBatchOwner(Long id) {
        TraceBatch batch = traceBatchMapper.selectById(id);
        if (batch == null) {
            throw new BusinessException("溯源批次不存在");
        }
        String role = UserContext.getRole();
        if (!"ADMIN".equals(role) && !batch.getFarmerId().equals(UserContext.getUserId())) {
            throw new BusinessException("无权操作该批次");
        }
        return batch;
    }

    private List<TraceNode> nodesOf(Long batchId) {
        return traceNodeMapper.selectList(new QueryWrapper<TraceNode>()
                .eq("batch_id", batchId)
                .orderByAsc("sort")
                .orderByAsc("id"));
    }

    private TraceBatchVO buildVO(TraceBatch batch) {
        TraceBatchVO vo = new TraceBatchVO();
        vo.setBatch(batch);
        List<TraceNode> nodes = nodesOf(batch.getId());
        vo.setNodes(nodes);

        // 哈希校验状态
        if (batch.getHashValue() == null || batch.getHashValue().isEmpty()) {
            vo.setHashStatus("NONE");
        } else {
            String current = computeChainHash(batch, nodes);
            vo.setHashStatus(batch.getHashValue().equalsIgnoreCase(current) ? "VERIFIED" : "TAMPERED");
        }

        // 环节进度引导：待完善正常引导；待审核时管理员补录质检/仓储/物流也需引导
        int bst = batch.getStatus() == null ? -1 : batch.getStatus();
        boolean guide = bst == 0 || (bst == 3 && "ADMIN".equals(UserContext.getRole()));
        int done = countNormalNodes(nodes);
        vo.setAllComplete(done >= NODE_ORDER.size());
        vo.setNextNodeType(guide && done < NODE_ORDER.size() ? NODE_ORDER.get(done) : null);
        vo.setScanCount(batch.getScanCount() == null ? 0 : batch.getScanCount());
        return vo;
    }

    private int countNormalNodes(List<TraceNode> nodes) {
        int n = 0;
        for (TraceNode node : nodes) {
            if (!NODE_CORRECTION.equals(node.getNodeType())) {
                n++;
            }
        }
        return n;
    }

    /**
     * 计算溯源链摘要哈希（SHA-256）。
     * 仅使用稳定字段（不含自增ID），同一批次内容在发布前后计算一致，可长期比对校验。
     */
    public String computeChainHash(TraceBatch batch, List<TraceNode> nodes) {
        return sha256(canonicalString(batch, nodes));
    }

    /** 溯源链规范化文本（哈希输入，可测试） */
    public String canonicalString(TraceBatch batch, List<TraceNode> nodes) {
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat timeFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        sb.append(nvl(batch.getBatchCode())).append('|')
                .append(nvl(batch.getProductName())).append('|')
                .append(batch.getCategoryId() == null ? "" : batch.getCategoryId()).append('|')
                .append(nvl(batch.getOrigin())).append('|')
                .append(batch.getFarmerId() == null ? "" : batch.getFarmerId()).append('|')
                .append(batch.getPlantDate() == null ? "" : dayFmt.format(batch.getPlantDate())).append('|')
                .append(batch.getHarvestDate() == null ? "" : dayFmt.format(batch.getHarvestDate())).append('|')
                .append(nvl(batch.getDescription())).append('|')
                .append(nvl(batch.getCoverImage()));
        for (TraceNode n : nodes) {
            sb.append('[')
                    .append(nvl(n.getNodeType())).append('|')
                    .append(nvl(n.getTitle())).append('|')
                    .append(nvl(n.getDescription())).append('|')
                    .append(nvl(n.getImages())).append('|')
                    .append(nvl(n.getOperator())).append('|')
                    .append(nvl(n.getLocation())).append('|')
                    .append(n.getRecordTime() == null ? "" : timeFmt.format(n.getRecordTime())).append('|')
                    .append(n.getSort() == null ? 0 : n.getSort())
                    .append(']');
        }
        return sb.toString();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new BusinessException("溯源链哈希计算失败");
        }
    }

    private String shortHash(String hash) {
        return hash == null || hash.length() <= 12 ? hash : hash.substring(0, 12) + "...";
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private String generateBatchCode() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        return "TR" + sdf.format(new Date()) + String.format("%03d", new Random().nextInt(1000));
    }
}
