package br.com.oficina.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("clienteSecurity")
public class ClienteSecurity {

    public boolean isClienteDoToken(Long clienteId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || clienteId == null) {
            return false;
        }

        boolean cliente = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CLIENTE".equals(authority.getAuthority()));
        if (!cliente || !(authentication.getDetails() instanceof JwtAuthenticationDetails details)) {
            return false;
        }

        return clienteId.equals(details.clienteId());
    }

    public boolean isDocumentoDoToken(String documento) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || documento == null) {
            return false;
        }

        boolean cliente = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CLIENTE".equals(authority.getAuthority()));
        if (!cliente) {
            return false;
        }

        String tokenDocumento = authentication.getName().replaceAll("\\D", "");
        String requestDocumento = documento.replaceAll("\\D", "");
        return tokenDocumento.equals(requestDocumento);
    }
}
