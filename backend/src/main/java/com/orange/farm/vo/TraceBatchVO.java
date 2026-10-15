package com.orange.farm.vo;

import com.orange.farm.entity.TraceBatch;
import com.orange.farm.entity.TraceNode;
import lombok.Data;

import java.util.List;

/**
 * 溯源批次展示对象（含环节链与可信校验信息）
 */
@Data
public class TraceBatchVO {

    private TraceBatch batch;
    private List<TraceNode> nodes;

    /** 哈希校验状态: VERIFIED校验通过 / TAMPERED疑似被篡改 / NONE未存证 */
    private String hashStatus;

    /** 本次查询是否为首次扫码 */
    private Boolean firstScan;

    /** 累计扫码次数 */
    private Integer scanCount;

    /** 高频扫码防伪预警（短时间内被反复扫描） */
    private Boolean highFreqWarning;

    /** 下一个应录入的环节类型（农户端引导），已录全或已发布时为 null */
    private String nextNodeType;

    /** 八大环节是否已录全（可提交审核） */
    private Boolean allComplete;
}
