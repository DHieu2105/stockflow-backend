package com.stockflow.modules.identity.repository;

import com.stockflow.modules.identity.entity.UserRole;
import com.stockflow.modules.identity.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByUser_Id(UUID userId);

    boolean existsByUser_IdAndRole_Code(UUID userId, String roleCode);
}

