package com.stockflow.modules.identity.service;

import com.stockflow.modules.identity.dto.RegisterRequest;
import com.stockflow.modules.identity.dto.UserResponse;
import com.stockflow.modules.identity.entity.Role;
import com.stockflow.modules.identity.entity.User;
import com.stockflow.modules.identity.entity.UserRole;
import com.stockflow.modules.identity.repository.RoleRepository;
import com.stockflow.modules.identity.repository.UserRepository;
import com.stockflow.modules.identity.repository.UserRoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public IdentityService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse registerCustomer(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Role customerRole = roleRepository.findByCode("CUSTOMER")
                .orElseThrow(() ->
                        new IllegalStateException("CUSTOMER role not found")
                );

        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User(
                request.email(),
                passwordHash,
                request.fullName()
        );

        User savedUser = userRepository.save(user);

        UserRole userRole = new UserRole(savedUser, customerRole);
        userRoleRepository.save(userRole);

        return UserResponse.from(savedUser);
    }
}
