package com.example.app.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.model.PasswordResetToken;
import com.example.app.model.User;
import com.example.app.repository.PasswordResetTokenRepository;
import com.example.app.repository.UserRepository;

@Service
public class PasswordResetService {
    public enum ResetResult { SUCCESS, INVALID_TOKEN, SAME_PASSWORD }

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String smtpHost;
    private final String frontendUrl;
    private final String fromAddress;
    private final long tokenLifetimeMinutes;

    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens,
            PasswordEncoder encoder, ObjectProvider<JavaMailSender> mailSenders,
            @Value("${spring.mail.host:}") String smtpHost,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.mail.from}") String fromAddress,
            @Value("${app.password-reset-token-minutes:15}") long tokenLifetimeMinutes) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.mailSenders = mailSenders;
        this.smtpHost = smtpHost;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
        this.fromAddress = fromAddress;
        if (tokenLifetimeMinutes <= 0) {
            throw new IllegalArgumentException("Password reset token lifetime must be positive");
        }
        this.tokenLifetimeMinutes = tokenLifetimeMinutes;
    }

    @Transactional
    public void requestReset(String email) {
        if (smtpHost.isBlank()) {
            throw new MailSendException("SMTP host is not configured; set MAIL_HOST to enable password reset emails");
        }
        JavaMailSender sender = mailSenders.getIfAvailable();
        if (sender == null) {
            throw new MailSendException("JavaMailSender is unavailable; check the mail configuration");
        }

        Optional<User> user = users.findByEmailIgnoreCase(email.trim());
        if (user.isEmpty() || !user.get().isEnabled()) return;

        byte[] randomToken = new byte[32];
        secureRandom.nextBytes(randomToken);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);
        String tokenHash = hash(rawToken);
        User account = user.get();

        tokens.deleteByUser(account);
        tokens.save(new PasswordResetToken(tokenHash, account,
                Instant.now().plus(tokenLifetimeMinutes, ChronoUnit.MINUTES)));

        String resetUrl = frontendUrl + "/reset-password?token=" + rawToken;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(account.getEmail());
        message.setSubject("Reset your ActivePass password");
        message.setText("Use this link to reset your password. It expires in "
                + tokenLifetimeMinutes + " minutes and can only be used once:\n\n" + resetUrl
                + "\n\nIf you did not request this, you can ignore this email.");
        sender.send(message);
    }

    @Transactional
    public void invalidateTokens(User user) {
        tokens.deleteByUser(user);
    }

    @Transactional
    public ResetResult resetPassword(String rawToken, String newPassword) {
        PasswordResetToken resetToken = tokens.findByTokenHash(hash(rawToken)).orElse(null);
        if (resetToken == null) return ResetResult.INVALID_TOKEN;
        if (!resetToken.getExpiresAt().isAfter(Instant.now())) {
            tokens.delete(resetToken);
            return ResetResult.INVALID_TOKEN;
        }

        User user = resetToken.getUser();
        if (encoder.matches(newPassword, user.getPassword())) return ResetResult.SAME_PASSWORD;
        user.setPassword(encoder.encode(newPassword));
        users.save(user);
        tokens.delete(resetToken);
        return ResetResult.SUCCESS;
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
