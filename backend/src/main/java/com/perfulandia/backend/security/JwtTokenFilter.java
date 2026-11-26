package com.perfulandia.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.*;

@Component
public class JwtTokenFilter extends OncePerRequestFilter {
    private final JwtService legacyJwtService;
    private final UserDetailsService userDetailsService;
    private final JwtDecoder keycloakDecoder;

    public JwtTokenFilter(JwtService legacyJwtService, UserDetailsService userDetailsService,
            @Value("${app.security.keycloak.jwk-set-uri}") String jwkSetUri,
            @Value("${app.security.keycloak.issuer-uri}") String issuerUri) {
        this.legacyJwtService = legacyJwtService;
        this.userDetailsService = userDetailsService;
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithIssuer(issuerUri));
        this.keycloakDecoder = decoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) { chain.doFilter(request, response); return; }
        String token = header.substring(7).trim();
        if (token.isEmpty()) { chain.doFilter(request, response); return; }
        try {
            Jwt jwt = keycloakDecoder.decode(token);
            Set<String> roles = new HashSet<>();
            Object realmAccess = jwt.getClaims().get("realm_access");
            if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> values)
                values.forEach(role -> roles.add(String.valueOf(role).toUpperCase(Locale.ROOT)));
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
            String username = jwt.getClaimAsString("preferred_username");
            if (username == null) username = jwt.getClaimAsString("email");
            Map<String, Object> principal = new HashMap<>(jwt.getClaims());
            principal.put("username", username);
            var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException keycloakFailure) {
            // Compatibilidad temporal con las cuentas/token HMAC creados por el login anterior.
            try {
                String username = legacyJwtService.extractUsername(token);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails details = userDetailsService.loadUserByUsername(username);
                    if (legacyJwtService.isTokenValid(token, details)) {
                        var auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (Exception ignored) {
                logger.debug("Bearer token inválido o expirado");
            }
        }
        chain.doFilter(request, response);
    }
}
