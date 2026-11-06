package com.stockflow.modules.identity.controller;

import com.stockflow.modules.identity.dto.RegisterRequest;
import com.stockflow.modules.identity.dto.LoginRequest;
import com.stockflow.modules.identity.dto.LoginResponse;
import com.stockflow.modules.identity.dto.UserResponse;
import com.stockflow.modules.identity.service.AuthService;
import com.stockflow.modules.identity.service.IdentityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class IdentityController {

    private final IdentityService identityService;
    private final AuthService authService;

    public IdentityController(IdentityService identityService, AuthService authService) {
        this.identityService = identityService;
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return identityService.registerCustomer(request);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(request);
    }
}

