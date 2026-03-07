package com.example.market.security.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access.expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh.expiration}")
    private long refreshExpiration;

    @Value("${jwt.issuer}")
    private String issuer;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Генерация Access токена
    public String generateAccessToken(Authentication authentication) {
        return generateToken(authentication, accessExpiration, "access");
    }

    // Генерация Refresh токена
    public String generateRefreshToken(Authentication authentication) {
        return generateToken(authentication, refreshExpiration, "refresh");
    }

    private String generateToken(Authentication authentication, long expiration, String type) {
        Map<String, Object> claims = new HashMap<>();

        // Добавляем authorities
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        claims.put("authorities", authorities);

        // Добавляем тип токена
        claims.put("type", type);

        // Добавляем дополнительные claims
        if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
            org.springframework.security.core.userdetails.User user =
                    (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
            claims.put("username", user.getUsername());
        }

        // Добавляем уникальный идентификатор токена
        claims.put("jti", UUID.randomUUID().toString());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(authentication.getName())
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Валидация токена
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Получение аутентификации из токена
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);

        String username = claims.getSubject();
        String authoritiesStr = claims.get("authorities", String.class);

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(authoritiesStr.split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        User principal = new User(username, "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    // Получение claims
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Получение имени пользователя из токена
    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    // Получение типа токена
    public String getTokenType(String token) {
        return getClaims(token).get("type", String.class);
    }

    // Получение JTI (JWT ID)
    public String getJti(String token) {
        return getClaims(token).get("jti", String.class);
    }

    // Проверка, является ли токен access токеном
    public boolean isAccessToken(String token) {
        return "access".equals(getTokenType(token));
    }

    // Проверка, является ли токен refresh токеном
    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenType(token));
    }

    // Получение времени истечения токена
    public Date getExpirationDate(String token) {
        return getClaims(token).getExpiration();
    }
}