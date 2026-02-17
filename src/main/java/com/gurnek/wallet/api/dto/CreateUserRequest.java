package com.gurnek.wallet.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank(message = "fullName is required")
        String fullName,
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email
) {
}
