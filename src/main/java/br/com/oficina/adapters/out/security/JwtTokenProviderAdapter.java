package br.com.oficina.adapters.out.security;

import br.com.oficina.application.port.out.TokenProviderPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProviderAdapter implements TokenProviderPort {

    private final SecretKey key;
    private final SecretKey clientKey;
    private final long expiration;
    private final String clientIssuer;
    private final String clientAudience;

    @Autowired
    public JwtTokenProviderAdapter(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration,
            @Value("${client.jwt.secret:}") String clientSecret,
            @Value("${client.jwt.issuer:}") String clientIssuer,
            @Value("${client.jwt.audience:}") String clientAudience) {
        this.key = Keys.hmacShaKeyFor(normalizeKey(secret));
        this.clientKey = clientSecret == null || clientSecret.isBlank()
                ? null
                : Keys.hmacShaKeyFor(normalizeKey(clientSecret));
        this.expiration = expiration;
        this.clientIssuer = clientIssuer;
        this.clientAudience = clientAudience;
    }

    JwtTokenProviderAdapter(String secret, long expiration) {
        this(secret, expiration, "", "", "");
    }

    private byte[] normalizeKey(String secret) {
        byte[] keyBytes = Optional.ofNullable(secret).orElse("").getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return keyBytes;
    }

    @Override
    public String gerarToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    @Override
    public String extrairUsername(String token) {
        return parseClaims(token).getSubject();
    }

    @Override
    public String extrairRole(String token) {
        Claims claims = parseClaims(token);
        String role = claims.get("role", String.class);
        return role != null ? role : claims.get("tipo", String.class);
    }

    @Override
    public Long extrairClienteId(String token) {
        Object clienteId = parseClaims(token).get("clienteId");
        if (clienteId instanceof Number number) {
            return number.longValue();
        }
        if (clienteId instanceof String text && !text.isBlank()) {
            return Long.valueOf(text);
        }
        return null;
    }

    @Override
    public boolean isTokenValido(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        JwtException internalException = null;
        try {
            return parseWithKey(token, key);
        } catch (JwtException | IllegalArgumentException e) {
            internalException = e instanceof JwtException jwtException
                    ? jwtException
                    : new JwtException(e.getMessage(), e);
        }

        if (clientKey != null) {
            try {
                Claims claims = parseWithKey(token, clientKey);
                validarClaimsCliente(claims);
                return claims;
            } catch (JwtException | IllegalArgumentException e) {
                throw e;
            }
        }

        throw internalException;
    }

    private Claims parseWithKey(String token, SecretKey secretKey) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private void validarClaimsCliente(Claims claims) {
        if (!"CLIENTE".equals(claims.get("tipo", String.class))) {
            throw new JwtException("JWT externo sem tipo CLIENTE");
        }
        if (clientIssuer != null && !clientIssuer.isBlank() && !clientIssuer.equals(claims.getIssuer())) {
            throw new JwtException("JWT externo com issuer invalido");
        }
        if (clientAudience != null && !clientAudience.isBlank()
                && (claims.getAudience() == null || !claims.getAudience().contains(clientAudience))) {
            throw new JwtException("JWT externo com audience invalida");
        }
    }
}
