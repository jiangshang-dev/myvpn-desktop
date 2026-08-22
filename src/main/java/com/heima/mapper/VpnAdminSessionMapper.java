package com.heima.mapper;

import com.heima.entity.VpnAdminSession;
import org.apache.ibatis.annotations.Param;

public interface VpnAdminSessionMapper {
    int insert(VpnAdminSession session);

    VpnAdminSession findByToken(@Param("token") String token);

    int deleteByToken(@Param("token") String token);
}
