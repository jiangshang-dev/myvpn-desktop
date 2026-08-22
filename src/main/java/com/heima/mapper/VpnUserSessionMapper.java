package com.heima.mapper;

import com.heima.entity.VpnUserSession;
import org.apache.ibatis.annotations.Param;

public interface VpnUserSessionMapper {
    int insert(VpnUserSession session);

    VpnUserSession findByToken(@Param("token") String token);

    int deleteByToken(@Param("token") String token);

    int deleteByDeviceId(@Param("deviceId") Long deviceId);
}
