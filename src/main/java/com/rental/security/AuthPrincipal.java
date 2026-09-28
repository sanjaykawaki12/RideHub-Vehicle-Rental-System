package com.rental.security;

// Lightweight principal carried inside the SecurityContext after JWT validation
public record AuthPrincipal(Long id, String email, String role) {}
