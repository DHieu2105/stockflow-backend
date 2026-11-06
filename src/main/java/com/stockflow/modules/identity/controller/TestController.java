package com.stockflow.modules.identity.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @GetMapping("/protected")
    public Map<String, Object> testProtected(Authentication authentication) {
        return Map.of(
                "message", "Access granted! You are authenticated.",
                "userId", authentication.getPrincipal(),
                "authorities", authentication.getAuthorities()
        );
    }
}

