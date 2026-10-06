package com.asjad.studygen.config;

import com.asjad.studygen.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


/**
 * JwtAuthFilter is a custom filter that extends OncePerRequestFilter to handle JWT-based authentication
 * by validating and parsing JWT tokens from incoming HTTP requests.
 *
 * The filter intercepts each request to check for an "Authorization" header containing a "Bearer" token,
 * extracts the JWT from the header, and validates it. If the token is valid, the filter sets the authenticated
 * user details in the SecurityContextHolder.
 *
 * This filter is used in conjunction with Spring Security to ensure that only requests with valid
 * JWT tokens can access protected resources.
 *
 * Key responsibilities:
 * - Extract the JWT from the "Authorization" header of HTTP requests.
 * - Retrieve and validate the user details from the JWT.
 * - Populate the SecurityContextHolder with an authenticated user if the token is valid.
 *
 * Dependencies:
 * - JwtService: Handles token parsing, validation, and extraction of claims (e.g., user email).
 * - UserDetailsServiceImpl: Loads user details by email retrieved from the token.
 *
 * Overrides:
 * - doFilterInternal: Implements the core logic for token validation and authentication setup.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String userEmail = jwtService.extractEmail(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Invalid, expired, or malformed JWT token
            logger.warn("JWT authentication failed: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}