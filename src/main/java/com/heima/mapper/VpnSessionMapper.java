package com.heima.mapper;

import com.heima.dto.AdminDtos;
import com.heima.entity.VpnSession;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface VpnSessionMapper {
    VpnSession findById(@Param("id") Long id);

    VpnSession findActiveByDevice(@Param("deviceId") Long deviceId);

    int insert(VpnSession session);

    int updateStatus(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("disconnectReason") String disconnectReason
    );

    int updateHeartbeat(
            @Param("id") Long id,
            @Param("uploadBytes") Long uploadBytes,
            @Param("downloadBytes") Long downloadBytes,
            @Param("uploadSpeed") Long uploadSpeed,
            @Param("downloadSpeed") Long downloadSpeed,
            @Param("clientIp") String clientIp
    );

    int markOnline(@Param("id") Long id, @Param("clientIp") String clientIp);

    long countOnline();

    long countOnlineByNode(@Param("nodeId") Long nodeId);

    long countOnlineByUser(@Param("userId") Long userId);

    List<AdminDtos.SessionRow> listOnlineSessions();

    List<AdminDtos.SessionRow> listSessions(@Param("status") String status, @Param("nodeId") Long nodeId, @Param("q") String q);

    List<AdminDtos.NodeOnlineRow> groupOnlineByNode();
}
