package com.example.market.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.market.model.SessionStatus;
import com.example.market.model.User;
import com.example.market.model.UserSession;
import com.example.market.repository.UserRepository;
import com.example.market.repository.UserSessionRepository;
import com.example.market.security.jwt.JwtTokenProvider;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;

    @Value("${jwt.refresh.expiration}")
    private long refreshExpiration;

    public TokenService(JwtTokenProvider jwtTokenProvider,
                        AuthenticationManager authenticationManager,
                        UserSessionRepository userSessionRepository,
                        UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.userSessionRepository = userSessionRepository;
        this.userRepository = userRepository;
    }

    // Аутентификация пользователя и выдача токенов
    @Transactional
    public TokenPair authenticate(String username, String password, HttpServletRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

        UserSession session = new UserSession(
                user,
                refreshToken,
                LocalDateTime.now().plus(Duration.ofMillis(refreshExpiration))
        );

        session.setIpAddress(request.getRemoteAddr());
        session.setUserAgent(request.getHeader("User-Agent"));

        userSessionRepository.save(session);

        return new TokenPair(
            accessToken,
            refreshToken,
            jwtTokenProvider.getExpirationDate(accessToken),
            jwtTokenProvider.getExpirationDate(refreshToken)
        );
    }

    @Transactional
    public TokenPair refreshTokens(String refreshToken, HttpServletRequest request) {
        if (!jwtTokenProvider.validateToken(refreshToken) ||
                !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        Optional<UserSession> sessionOpt = userSessionRepository.findByRefreshToken(refreshToken);
        if (sessionOpt.isEmpty()) {
            throw new RuntimeException("Session not found");
        }

        UserSession session = sessionOpt.get();

        if (!session.isActive()) {
            throw new RuntimeException("Session is not active");
        }

        session.setStatus(SessionStatus.REFRESHED);
        userSessionRepository.save(session);

        User user = session.getUser();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                user.getAuthorities()
        );

        String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        UserSession newSession = new UserSession(
                user,
                newRefreshToken,
                LocalDateTime.now().plus(Duration.ofMillis(refreshExpiration))
        );

        newSession.setIpAddress(request.getRemoteAddr());
        newSession.setUserAgent(request.getHeader("User-Agent"));

        userSessionRepository.save(newSession);

    return new TokenPair(
        newAccessToken,
        newRefreshToken,
        jwtTokenProvider.getExpirationDate(newAccessToken),
        jwtTokenProvider.getExpirationDate(newRefreshToken)
    );
    }

    @Transactional
    public void logout(String refreshToken) {
        Optional<UserSession> sessionOpt = userSessionRepository.findByRefreshToken(refreshToken);
        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            session.revoke();
            userSessionRepository.save(session);
        }
    }

    @Transactional
    public void logoutAllSessions(Long userId) {
        userSessionRepository.updateStatusForUser(
                userId,
                SessionStatus.ACTIVE,
                SessionStatus.REVOKED
        );
    }

    @Transactional
    public void logoutAllSessions(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        logoutAllSessions(user.getId());
    }

    public List<UserSession> getActiveSessions(Long userId) {
        return userSessionRepository.findByUserIdAndStatus(userId, SessionStatus.ACTIVE);
    }

    public List<UserSession> getActiveSessions(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return getActiveSessions(user.getId());
    }

    @Transactional
    public void cleanupExpiredSessions() {
        List<UserSession> expiredSessions = userSessionRepository
                .findByStatusAndExpiresAtBefore(
                        SessionStatus.ACTIVE,
                        LocalDateTime.now()
                );

        for (UserSession session : expiredSessions) {
            session.setStatus(SessionStatus.EXPIRED);
        }

        userSessionRepository.saveAll(expiredSessions);
    }

    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;
        private final Date accessTokenExpiry;
        private final Date refreshTokenExpiry;

        public TokenPair(String accessToken, String refreshToken, Date accessTokenExpiry, Date refreshTokenExpiry) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.accessTokenExpiry = accessTokenExpiry;
            this.refreshTokenExpiry = refreshTokenExpiry;
        }

        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public Date getAccessTokenExpiry() { return accessTokenExpiry; }
        public Date getRefreshTokenExpiry() { return refreshTokenExpiry; }
    }
}