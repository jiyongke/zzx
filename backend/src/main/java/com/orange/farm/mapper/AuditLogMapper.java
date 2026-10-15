package com.orange.farm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orange.farm.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
