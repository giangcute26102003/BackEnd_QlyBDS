package com.example.datn_realeaste_crm.security;

import com.example.datn_realeaste_crm.entity.Token;
import com.example.datn_realeaste_crm.entity.User;
import com.example.datn_realeaste_crm.repository.TokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {

    private Key key;
    private final TokenRepository tokenRepository;
    private final UserDetailsService userDetailsService;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Autowired
    public JwtTokenProvider(TokenRepository tokenRepository, UserDetailsService userDetailsService) {
        this.tokenRepository = tokenRepository;
        this.userDetailsService = userDetailsService;
    }

    @PostConstruct
    public void init() {
        try {
            if (secretKey == null || secretKey.trim().isEmpty()) {
                throw new IllegalArgumentException("JWT secret key cannot be null or empty");
            }
            byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            this.key = Keys.hmacShaKeyFor(keyBytes);
            log.info("JWT key initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize JWT key: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize JWT key", e);
        }
    }

    /**
     * Generate access token with role and permissions
     * @param username email của user
     * @param role vai trò được chọn
     * @param permissions danh sách quyền tương ứng với role
     * @return JWT access token
     */
    public String generateAccessToken(String username, String role, Set<String> permissions) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }
        if (permissions == null) {
            permissions = Collections.emptySet();
        }

        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenExpiration);

        try {
            return Jwts.builder()
                    .setSubject(username)
                    .claim("role", role)
                    .claim("permissions", new ArrayList<>(permissions)) // Convert to List for consistent serialization
                    .claim("type", "access")
                    .setIssuedAt(now)
                    .setExpiration(validity)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            log.error("Failed to generate access token for user: {}", username, e);
            throw new RuntimeException("Failed to generate access token", e);
        }
    }

    /**
     * Generate refresh token with role information
     * @param user User entity
     * @param role selected role
     * @return JWT refresh token
     */
    public String generateRefreshToken(User user, String role) {
        if (user == null || user.getEmail() == null) {
            throw new IllegalArgumentException("User and user email cannot be null");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }

        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenExpiration);

        try {
            String refreshToken = Jwts.builder()
                    .setSubject(user.getEmail())
                    .claim("role", role)
                    .claim("type", "refresh")
                    .setIssuedAt(now)
                    .setExpiration(validity)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();

            // Save token to database
            saveToken(user, refreshToken, "refresh", validity);
            return refreshToken;
        } catch (Exception e) {
            log.error("Failed to generate refresh token for user: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to generate refresh token", e);
        }
    }

    /**
     * Extract role from JWT token
     * @param token JWT token
     * @return role name
     */
    public String getRoleFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        try {
            Claims claims = parseToken(token);
            return claims.get("role", String.class);
        } catch (Exception e) {
            log.error("Failed to extract role from token", e);
            throw new RuntimeException("Failed to extract role from token", e);
        }
    }

    /**
     * Extract permissions from JWT token
     * @param token JWT token
     * @return set of permissions
     */
    @SuppressWarnings("unchecked")
    public Set<String> getPermissionsFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        try {
            Claims claims = parseToken(token);
            Object permissionsClaim = claims.get("permissions");
            
            if (permissionsClaim instanceof List) {
                return ((List<String>) permissionsClaim).stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            } else if (permissionsClaim instanceof Collection) {
                return ((Collection<?>) permissionsClaim).stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .collect(Collectors.toSet());
            }
            
            return Collections.emptySet();
        } catch (Exception e) {
            log.error("Failed to extract permissions from token", e);
            return Collections.emptySet();
        }
    }

    /**
     * Get Authentication object from token
     * @param token JWT token
     * @return Authentication object
     */
    public Authentication getAuthentication(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        try {
            Claims claims = parseToken(token);
            String username = claims.getSubject();
            
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("Token subject cannot be null or empty");
            }

            // Extract authorities from role and permissions
            Collection<GrantedAuthority> authorities = extractAuthorities(claims);
            
            log.debug("Extracted authorities for user {}: {}", username, authorities);
            return new UsernamePasswordAuthenticationToken(username, null, authorities);
        } catch (Exception e) {
            log.error("Failed to get authentication from token", e);
            throw new RuntimeException("Failed to get authentication from token", e);
        }
    }

    /**
     * Validate JWT token
     * @param token JWT token
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Token is null or empty");
            return false;
        }
        try {
            // Parse and validate token signature and expiration
            parseToken(token);
            // Check if token is revoked
            if (tokenRepository.findByTokenAndRevokedTrue(token).isPresent()) {
                log.warn("Token is revoked");
                return false;
            }
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Revoke a token
     * @param token JWT token to revoke
     */
    public void revokeToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Cannot revoke null or empty token");
            return;
        }

        try {
            tokenRepository.findByToken(token).ifPresentOrElse(
                tokenEntity -> {
                    tokenEntity.setRevoked(true);
                    tokenRepository.save(tokenEntity);
                    log.info("Token revoked successfully");
                },
                () -> log.warn("Token not found in database for revocation")
            );
        } catch (Exception e) {
            log.error("Failed to revoke token", e);
        }
    }

    /**
     * Extract username from token
     * @param token JWT token
     * @return username
     */
    public String getUsernameFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        try {
            Claims claims = parseToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.error("Failed to extract username from token", e);
            throw new RuntimeException("Failed to extract username from token", e);
        }
    }

    /**
     * Check if token is refresh token
     * @param token JWT token
     * @return true if refresh token, false otherwise
     */
    public boolean isRefreshToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            Claims claims = parseToken(token);
            return "refresh".equals(claims.get("type"));
        } catch (Exception e) {
            log.warn("Failed to check if token is refresh token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Parse JWT token and extract claims
     * @param token JWT token
     * @return Claims object
     */
    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extract authorities from token claims
     * @param claims JWT claims
     * @return Collection of GrantedAuthority
     */
    private Collection<GrantedAuthority> extractAuthorities(Claims claims) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Add role as authority with ROLE_ prefix
        String role = claims.get("role", String.class);
        if (role != null && !role.trim().isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
        }

        // Add permissions as authorities
        Object permissionsClaim = claims.get("permissions");
        if (permissionsClaim instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> permissions = (List<String>) permissionsClaim;
            permissions.stream()
                    .filter(Objects::nonNull)
                    .filter(perm -> !perm.trim().isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        return authorities;
    }

    /**
     * Save token to database
     * @param user User entity
     * @param token JWT token string
     * @param tokenType Type of token (access/refresh)
     * @param validity Expiration date
     */
    private void saveToken(User user, String token, String tokenType, Date validity) {
        try {
            Token tokenEntity = new Token();
            tokenEntity.setUser(user);
            tokenEntity.setToken(token);
            tokenEntity.setTokenType(tokenType);
            tokenEntity.setExpirationDate(LocalDateTime.ofInstant(validity.toInstant(), ZoneId.systemDefault()));
            tokenRepository.save(tokenEntity);
            log.debug("Token saved to database for user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to save token to database for user: {}", user.getEmail(), e);
            // Don't throw exception here as token generation should still succeed
        }
    }
}