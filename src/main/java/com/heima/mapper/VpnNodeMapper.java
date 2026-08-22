package com.heima.mapper;

import com.heima.dto.AdminDtos;
import com.heima.entity.VpnNode;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface VpnNodeMapper {
    VpnNode findById(@Param("id") Long id);

    List<VpnNode> listEnabled();

    List<AdminDtos.NodeRow> listAll();

    int insert(VpnNode node);

    int update(VpnNode node);

    int updateEnabled(@Param("id") Long id, @Param("enabled") Integer enabled);

    long countEnabled();
}
