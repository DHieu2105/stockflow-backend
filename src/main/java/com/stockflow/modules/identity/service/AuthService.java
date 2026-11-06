package com.stockflow.modules.identity.service;

import com.stockflow.modules.identity.dto.LoginRequest;
import com.stockflow.modules.identity.dto.LoginResponse;
import com.stockflow.modules.identity.dto.UserResponse;
import com.stockflow.modules.identity.entity.User;
import com.stockflow.modules.identity.entity.UserRole;
import com.stockflow.modules.identity.entity.UserStatus;
import com.stockflow.modules.identity.repository.UserRepository;
import com.stockflow.modules.identity.repository.UserRoleRepository;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        // 1. Tìm tài khoản theo email
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid email or password")
                );

        // 2. Kiểm tra trạng thái tài khoản
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException("Invalid email or password");
        }

        // 3. Kiểm tra mật khẩu
        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new BadCredentialsException("Invalid email or password");
        }

        // 4. Lấy danh sách quyền của User
        List<String> roles = userRoleRepository.findByUser_Id(user.getId())
                .stream()
                .map(UserRole::getRole)
                .map(role -> role.getCode())
                .toList();

        // 5. Tạo JWT
        String accessToken = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                roles
        );

        // 6. Trả kết quả
        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtService.getAccessTokenExpiration(),
                UserResponse.from(user)
        );
    }
}

