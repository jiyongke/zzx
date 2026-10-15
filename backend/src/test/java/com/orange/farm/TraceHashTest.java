package com.orange.farm;

import com.orange.farm.entity.TraceBatch;
import com.orange.farm.entity.TraceNode;
import com.orange.farm.service.TraceService;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 溯源链 SHA-256 摘要哈希算法一致性测试：
 * 与 sql/orange_farm.sql 中种子批次的哈希存证结果互相验证，
 * 确保「发布时计算入库」与「查询时重算比对」使用同一算法。
 */
public class TraceHashTest {

    private final TraceService traceService = new TraceService();

    @Test
    public void seedBatch1HashMatchesSql() throws Exception {
        SimpleDateFormat day = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        TraceBatch b = new TraceBatch();
        b.setBatchCode("TR20260901001");
        b.setProductName("赣南脐橙");
        b.setCategoryId(1L);
        b.setOrigin("江西省赣州市信丰县安西镇");
        b.setFarmerId(2L);
        b.setPlantDate(day.parse("2026-03-15"));
        b.setHarvestDate(day.parse("2026-09-20"));
        b.setDescription("选用赣南红壤丘陵种植的纽荷尔脐橙，山泉水灌溉，施用农家有机肥，全程可溯源。");
        b.setCoverImage("http://localhost:8080/images/orange.jpg");

        List<TraceNode> nodes = new ArrayList<>();
        add(nodes, time, "PLANT", "选种育苗",
                "选用纽荷尔脐橙脱毒苗，基地大棚集中育苗，苗龄8个月以上。",
                "https://picsum.photos/seed/nursery1/800/600", "赣南脐橙合作社", "江西信丰安西镇育苗基地", "2026-03-15 08:30:00", 1);
        add(nodes, time, "FERTILIZE", "有机施肥",
                "每亩施用腐熟农家肥2.5吨，配合生物菌肥改良土壤，不使用化学激素。",
                "https://picsum.photos/seed/fertilize1/800/600", "赣南脐橙合作社", "江西信丰安西镇果园", "2026-04-02 09:00:00", 2);
        add(nodes, time, "PEST", "生物防治",
                "采用黄板、性诱剂等绿色防控技术，全程零高毒农药，农残检测合格。",
                "https://picsum.photos/seed/pest1/800/600", "赣南脐橙合作社", "江西信丰安西镇果园", "2026-06-20 10:00:00", 3);
        add(nodes, time, "HARVEST", "人工采收",
                "糖度达到12°以上后人工采摘，果柄保留完整，当日运输入库。",
                "https://picsum.photos/seed/harvest1/800/600", "赣南脐橙合作社", "江西信丰安西镇果园", "2026-09-20 07:00:00", 4);
        add(nodes, time, "PROCESS", "分级分拣",
                "按果径60-85mm分级，人工挑选病果、畸形果，清水冲洗后打蜡保鲜。",
                "https://picsum.photos/seed/process1/800/600", "橙心助农分拣中心", "江西信丰加工厂", "2026-09-21 14:00:00", 5);
        add(nodes, time, "QUALITY", "质检认证",
                "第三方机构抽检：糖度12.8°，农残检测全部未检出，出具质检合格报告。",
                "https://picsum.photos/seed/quality1/800/600", "橙心助农质检中心", "江西信丰检测实验室", "2026-09-22 10:00:00", 6);
        add(nodes, time, "STORAGE", "冷库保鲜",
                "分选后入冷库 4-6℃ 预冷保鲜，库内温度实时监控记录。",
                "https://picsum.photos/seed/storage1/800/600", "橙心助农仓储中心", "江西信丰冷库", "2026-09-22 15:00:00", 7);
        add(nodes, time, "LOGISTICS", "冷链发货",
                "全程冷链车运输，恒温8-10℃，48小时内送达，附运输轨迹单。",
                "https://picsum.photos/seed/logistics1/800/600", "顺源冷链物流", "信丰-全国冷链干线", "2026-09-23 06:00:00", 8);

        String hash = traceService.computeChainHash(b, nodes);
        assertEquals("4114ada9acb69f4e1b05e64c243e20bae06a499cd643f4240017300e29526d4b", hash,
                "批次1哈希必须与 sql/orange_farm.sql 中存证的 hash_value 一致");
    }

    private void add(List<TraceNode> nodes, SimpleDateFormat time, String type, String title,
                     String desc, String images, String operator, String location,
                     String recordTime, int sort) throws Exception {
        TraceNode n = new TraceNode();
        n.setNodeType(type);
        n.setTitle(title);
        n.setDescription(desc);
        n.setImages(images);
        n.setOperator(operator);
        n.setLocation(location);
        n.setRecordTime(time.parse(recordTime));
        n.setSort(sort);
        nodes.add(n);
    }
}
