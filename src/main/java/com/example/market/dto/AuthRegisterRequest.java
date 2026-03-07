package com.example.market.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @Email @NotBlank String email,
        @NotBlank String password
) {
}