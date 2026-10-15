package com.orange.farm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orange.farm.entity.TraceBatch;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface TraceBatchMapper extends BaseMapper<TraceBatch> {

    /** 按状态统计批次数量 */
    @Select("SELECT status AS name, COUNT(*) AS value FROM trace_batch GROUP BY status")
    List<Map<String, Object>> countByStatus();
}
