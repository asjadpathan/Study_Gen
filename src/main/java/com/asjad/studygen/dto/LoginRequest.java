package com.asjad.studygen.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * A record that represents a login request for an existing user.
 *
 * This record encapsulates the user's email and password required for authentication.
 *
 * Validation constraints ensure that:
 * - email must be a valid email format and must not be blank.
 * - password must not be blank.
 *
 * This record is typically used in the authentication process
 * to verify user credentials and generate an authentication token.
 */
public record LoginRequest(
        @NotBlank @Email
        String email,

        @NotBlank
        String password
) {}