package com.hydrospark.billing.service;

import com.hydrospark.billing.dto.LoginRequest;
import com.hydrospark.billing.dto.LoginResponse;
import com.hydrospark.billing.model.User;
import com.hydrospark.billing.repository.UserRepository;
import com.hydrospark.billing.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    private static final int MAX_FAILED_ATTEMPTS = 10;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getEmail());

        // Find user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Check if account is locked
        if (isAccountLocked(user)) {
            throw new RuntimeException("Account is locked due to too many failed login attempts. Please try again later.");
        }

        // Check if account is active
        if (!user.getIsActive()) {
            throw new RuntimeException("Account is disabled. Please contact support.");
        }

        try {
            // Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate tokens
            String accessToken = tokenProvider.generateAccessToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(authentication);

            // Reset failed login attempts on successful login
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("Login successful for user: {}", request.getEmail());

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .customerId(user.getCustomerId())
                    .build();

        } catch (Exception e) {
            // Increment failed login attempts
            handleFailedLogin(user);
            throw new RuntimeException("Invalid email or password");
        }
    }

    @Transactional
    public LoginResponse refreshToken(String refreshToken) {
        log.info("Refreshing access token");

        // Validate refresh token
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        // Extract email from token
        String email = tokenProvider.getEmailFromToken(refreshToken);

        // Find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if account is active
        if (!user.getIsActive()) {
            throw new RuntimeException("Account is disabled");
        }

        // Create new authentication
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                email,
                null,
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_" + user.getRole().name())
        );

        // Generate new access token
        String newAccessToken = tokenProvider.generateAccessToken(authentication);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Return same refresh token
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .customerId(user.getCustomerId())
                .build();
    }

    @Transactional
    public void logout() {
        SecurityContextHolder.clearContext();
        log.info("User logged out");
    }

    @Transactional
    public void changePassword(String userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Validate new password strength
        validatePasswordStrength(newPassword);

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed for user: {}", user.getEmail());
    }

    @Transactional
    public void resetPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate temporary password (in production, send via email)
        String tempPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        log.info("Password reset for user: {}. Temporary password: {}", email, tempPassword);
        // TODO: Send email with temporary password
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
            log.warn("Account locked for user: {} due to {} failed attempts", user.getEmail(), attempts);
        }

        userRepository.save(user);
    }

    private boolean isAccountLocked(User user) {
        if (user.getAccountLockedUntil() == null) {
            return false;
        }

        if (LocalDateTime.now().isBefore(user.getAccountLockedUntil())) {
            return true;
        }

        // Lock duration has passed, reset
        user.setAccountLockedUntil(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        return false;
    }

    private void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters long");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new RuntimeException("Password must contain at least one uppercase letter");
        }

        if (!password.matches(".*[a-z].*")) {
            throw new RuntimeException("Password must contain at least one lowercase letter");
        }

        if (!password.matches(".*\\d.*")) {
            throw new RuntimeException("Password must contain at least one digit");
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new RuntimeException("Password must contain at least one special character");
        }
    }

    private String generateTemporaryPassword() {
        // Simple temporary password generator
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email).orElse(null);
    }
}
