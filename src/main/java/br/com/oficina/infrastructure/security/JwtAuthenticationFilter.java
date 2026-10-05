package br.com.oficina.infrastructure.security;

import br.com.oficina.application.port.out.TokenProviderPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProviderPort tokenProviderPort;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            autenticarToken(request, token);
        }

        filterChain.doFilter(request, response);
    }

    private void autenticarToken(HttpServletRequest request, String token) {
        try {
            if (!tokenProviderPort.isTokenValido(token)) {
                return;
            }

            String username = tokenProviderPort.extrairUsername(token);
            String role = tokenProviderPort.extrairRole(token);

            if (username == null || role == null) {
                return;
            }

            var authority = new SimpleGrantedAuthority("ROLE_" + role);
            var auth = new UsernamePasswordAuthenticationToken(username, null, List.of(authority));
            var webDetails = new WebAuthenticationDetailsSource().buildDetails(request);
            auth.setDetails(new JwtAuthenticationDetails(webDetails, tokenProviderPort.extrairClienteId(token)));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (RuntimeException ignored) {
            SecurityContextHolder.clearContext();
        }
    }
}
