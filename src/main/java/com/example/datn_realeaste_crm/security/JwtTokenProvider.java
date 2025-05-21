package com.example.datn_realeaste_crm.security;

import com.example.datn_realeaste_crm.entity.Token;
import com.example.datn_realeaste_crm.entity.User;
import com.example.datn_realeaste_crm.repository.TokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {

    private Key key;

    private final TokenRepository tokenRepository;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Autowired
    public JwtTokenProvider(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @PostConstruct
    public void init() {
        try {
            // Sử dụng string trực tiếp thay vì giải mã BASE64
            byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            this.key = Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            log.error("Failed to initialize JWT key: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize JWT key", e);
        }
    }

    public String createAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenExpiration);

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("auth", authorities)
                .claim("type", "access")
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenExpiration);

        String refreshToken = Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("type", "refresh")
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // Save token to database
        saveToken((User) userDetails, refreshToken, "refresh", validity);

        return refreshToken;
    }

    private void saveToken(User user, String token, String tokenType, Date validity) {
        Token tokenEntity = new Token();
        tokenEntity.setUser(user);
        tokenEntity.setToken(token);
        tokenEntity.setTokenType(tokenType);
        tokenEntity.setExpirationDate(LocalDateTime.ofInstant(validity.toInstant(), ZoneId.systemDefault()));
        tokenRepository.save(tokenEntity);
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Collection<GrantedAuthority> authorities = new ArrayList<>();

        // Check if "auth" claim exists
        if (claims.containsKey("auth")) {
            Object authClaim = claims.get("auth");
            log.debug("Auth claim type: {}", authClaim.getClass().getName());

            if (authClaim instanceof List) {
                List<String> roles = claims.get("auth", List.class);
                authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            } else if (authClaim instanceof LinkedHashMap || authClaim instanceof Map) {
                // Handle case where auth is a Map (common with Sets serialized to JSON)
                Collection<?> values = ((Map<?, ?>) authClaim).values();
                authorities = values.stream()
                        .map(Object::toString)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            } else if (authClaim instanceof String) {
                // Handle case where auth is a comma-separated string
                authorities = Arrays.stream(authClaim.toString().split(","))
                        .filter(auth -> !auth.trim().isEmpty())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            } else if (authClaim instanceof Collection) {
                // Handle case where auth might be a Set or another collection type
                authorities = ((Collection<?>) authClaim).stream()
                        .map(Object::toString)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }

            log.debug("Extracted authorities: {}", authorities);
        } else {
            log.warn("No 'auth' claim found in token");
        }

        return new UsernamePasswordAuthenticationToken(claims.getSubject(), "", authorities);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);

            // Check if token is revoked
            if (tokenRepository.findByTokenAndRevokedTrue(token).isPresent()) {
                return false;
            }

            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public void revokeToken(String token) {
        tokenRepository.findByToken(token).ifPresent(tokenEntity -> {
            tokenEntity.setRevoked(true);
            tokenRepository.save(tokenEntity);
        });
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // public boolean isRefreshToken(String token) {
    // Claims claims = Jwts.parserBuilder()
    // .setSigningKey(key)
    // .build()
    // .parseClaimsJws(token)
    // .getBody();
    //
    // return "refresh".equals(claims.get("type"));
    // }
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return "refresh".equals(claims.get("type"));
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid or expired JWT while checking refresh token: {}", e.getMessage());
            return false;
        }
    }
}