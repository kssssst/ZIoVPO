package com.example.market.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        return ResponseEntity.ok(Map.of(
                "project", "ZIoVPO Assignment 1 backend",
                "features", new String[]{"JWT access/refresh", "role-based authorization", "PostgreSQL", "HTTPS profile"},
                "publicEndpoints", new String[]{"POST /auth/register", "POST /auth/login", "POST /auth/refresh"}
        ));
    }
}
