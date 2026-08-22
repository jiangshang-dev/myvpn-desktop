package com.heima.mapper;

import com.heima.dto.AdminDtos;
import com.heima.entity.VpnUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface VpnUserMapper {
    VpnUser findByEmail(@Param("email") String email);

    VpnUser findById(@Param("id") Long id);

    int insert(VpnUser user);

    int updateLastLogin(@Param("id") Long id);

    int updateEnabled(@Param("id") Long id, @Param("enabled") Integer enabled);

    long countAll();

    List<AdminDtos.UserRow> listUsers(@Param("q") String q);
}
