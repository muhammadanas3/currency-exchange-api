package com.currency.dto;

import com.currency.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuthRequest {
    @NotBlank
    private String username;
    
    @NotBlank
    private String password;

    @NotBlank
    private String email;

    @NotNull
    private Role role;
} 