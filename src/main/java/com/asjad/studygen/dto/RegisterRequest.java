package com.asjad.studygen.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A record that represents a registration request for a new user.
 *
 * This record is used to encapsulate the data required for registering
 * a user, including their full name, email, and password.
 *
 * Validation constraints enforce that:
 * - fullName must not be blank.
 * - email must be a valid email address and must not be blank.
 * - password must not be blank and must be at least 8 characters long.
 *
 * The validation annotations ensure that the data meets these requirements
 * before processing the registration request.
 */
public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank @Email(message = "Valid email is required")
        String email,

        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {}