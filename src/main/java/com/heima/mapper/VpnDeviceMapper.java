package com.heima.mapper;

import com.heima.dto.AdminDtos;
import com.heima.entity.VpnDevice;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface VpnDeviceMapper {
    VpnDevice findByUserAndUuid(@Param("userId") Long userId, @Param("deviceUuid") String deviceUuid);

    VpnDevice findById(@Param("id") Long id);

    int insert(VpnDevice device);

    int updateLastSeen(@Param("id") Long id, @Param("lastIp") String lastIp);

    int updateEnabled(@Param("id") Long id, @Param("enabled") Integer enabled);

    long countByUser(@Param("userId") Long userId);

    List<AdminDtos.DeviceRow> listDevices(@Param("q") String q, @Param("userId") Long userId);
}
