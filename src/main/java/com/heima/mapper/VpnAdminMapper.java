package com.heima.mapper;

import com.heima.entity.VpnAdmin;
import org.apache.ibatis.annotations.Param;

public interface VpnAdminMapper {
    VpnAdmin findByAccount(@Param("account") String account);

    VpnAdmin findById(@Param("id") Long id);

    int insert(VpnAdmin admin);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    long countAll();
}
