package br.com.oficina.infrastructure.security;

import org.springframework.security.web.authentication.WebAuthenticationDetails;

public record JwtAuthenticationDetails(
        WebAuthenticationDetails webDetails,
        Long clienteId
) {
}
