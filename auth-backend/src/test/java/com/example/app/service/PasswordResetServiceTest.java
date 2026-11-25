package com.example.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.app.model.PasswordResetToken;
import com.example.app.model.Role;
import com.example.app.model.User;
import com.example.app.repository.PasswordResetTokenRepository;
import com.example.app.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {
    @Mock private UserRepository users;
    @Mock private PasswordResetTokenRepository tokens;
    @Mock private PasswordEncoder encoder;
    @Mock private ObjectProvider<JavaMailSender> mailSenders;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(users, tokens, encoder, mailSenders,
                "smtp.gmail.com", "http://localhost:5173", "no-reply@example.com", 15);
    }

    @Test
    void rejectsResetRequestBeforeAccountLookupWhenSmtpHostIsMissing() {
        service = new PasswordResetService(users, tokens, encoder, mailSenders,
                " ", "http://localhost:5173", "no-reply@example.com", 15);

        assertThrows(MailSendException.class, () -> service.requestReset("user@example.com"));
        verifyNoInteractions(users, tokens, mailSenders);
    }

    @Test
    void consumesResetTokenAndUpdatesPassword() throws Exception {
        String rawToken = "aValidResetToken";
        String tokenHash = hash(rawToken);
        User user = new User("Test User", "user@example.com", "old-hash", Role.STUDENT);
        PasswordResetToken resetToken = new PasswordResetToken(
                tokenHash, user, Instant.now().plus(15, ChronoUnit.MINUTES));
        when(tokens.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));
        when(encoder.matches("new-password", "old-hash")).thenReturn(false);
        when(encoder.encode("new-password")).thenReturn("new-hash");

        PasswordResetService.ResetResult result = service.resetPassword(rawToken, "new-password");

        assertEquals(PasswordResetService.ResetResult.SUCCESS, result);
        assertEquals("new-hash", user.getPassword());
        verify(users).save(user);
        verify(tokens).delete(resetToken);
    }

    @Test
    void rejectsExpiredTokenAndDeletesIt() throws Exception {
        String rawToken = "expiredResetToken";
        String tokenHash = hash(rawToken);
        PasswordResetToken resetToken = new PasswordResetToken(
                tokenHash, new User("Test User", "user@example.com", "old-hash", Role.STUDENT),
                Instant.now().minus(1, ChronoUnit.SECONDS));
        when(tokens.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));

        PasswordResetService.ResetResult result = service.resetPassword(rawToken, "new-password");

        assertEquals(PasswordResetService.ResetResult.INVALID_TOKEN, result);
        verify(tokens).delete(resetToken);
    }

    private String hash(String token) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}
