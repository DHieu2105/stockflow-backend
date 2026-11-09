package com.stockflow.modules.warehouse.service;

import com.stockflow.modules.warehouse.repository.WarehouseStaffRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("warehouseAuthorization")
public class WarehouseAuthorizationService {

    private final WarehouseStaffRepository warehouseStaffRepository;

    public WarehouseAuthorizationService(
            WarehouseStaffRepository warehouseStaffRepository
    ) {
        this.warehouseStaffRepository = warehouseStaffRepository;
    }

    public boolean hasAccess(UUID warehouseId) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // ADMIN được truy cập tất cả kho
        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                );

        if (isAdmin) {
            return true;
        }

        boolean canOperateWarehouse = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_WAREHOUSE_MANAGER")
                        || authority.getAuthority().equals("ROLE_WAREHOUSE_STAFF")
                );

        if (!canOperateWarehouse) {
            return false;
        }

        // Lấy UUID của user từ JWT Filter
        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UUID userId)) {
            return false;
        }

        // Chỉ cho phép nếu user được phân công vào warehouse
        return warehouseStaffRepository
                .existsById_WarehouseIdAndId_UserId(
                        warehouseId,
                        userId
                );
    }

    public void checkAccess(UUID warehouseId) {
        if (!hasAccess(warehouseId)) {
            throw new AccessDeniedException(
                    "You do not have permission to access this warehouse"
            );
        }
    }
}

