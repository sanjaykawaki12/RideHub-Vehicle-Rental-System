package com.rental.dto;

import com.rental.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank
    private String password;

    @NotNull
    private Role role; // USER or OWNER
}
