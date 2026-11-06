package com.stockflow.modules.identity.controller;

import com.stockflow.modules.identity.dto.RegisterRequest;
import com.stockflow.modules.identity.dto.UserResponse;
import com.stockflow.modules.identity.service.IdentityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class IdentityController {

    private final IdentityService identityService;

    public IdentityController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return identityService.registerCustomer(request);
    }
}

