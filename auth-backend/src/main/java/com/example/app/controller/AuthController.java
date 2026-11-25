package com.example.app.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.mail.MailException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.dto.ForgotPasswordRequest;
import com.example.app.dto.LoginRequest;
import com.example.app.dto.PasswordChangeRequest;
import com.example.app.dto.ProfileUpdateRequest;
import com.example.app.dto.RegisterRequest;
import com.example.app.dto.ResetPasswordRequest;
import com.example.app.dto.UserResponse;
import com.example.app.model.Role;
import com.example.app.model.User;
import com.example.app.repository.UserRepository;
import com.example.app.service.PasswordResetService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authManager;
    private final SecurityContextRepository contextRepo;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final PasswordResetService passwordResets;

    public AuthController(AuthenticationManager authManager, SecurityContextRepository contextRepo,
            UserRepository users, PasswordEncoder encoder, PasswordResetService passwordResets) {
        this.authManager = authManager;
        this.contextRepo = contextRepo;
        this.users = users;
        this.encoder = encoder;
        this.passwordResets = passwordResets;
    }

    /**
     * Call once on app start so the browser receives the XSRF-TOKEN cookie.
     */
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req,
            HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication auth = authManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(req.email().trim(), req.password()));

            // Prevent session fixation: new session ID once authenticated
            if (request.getSession(false) != null) {
                request.changeSessionId();
            }

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            contextRepo.saveContext(context, request, response); // stores it in the HttpSession

            User user = users.findByEmailIgnoreCase(auth.getName()).orElseThrow();
            return ResponseEntity.ok(UserResponse.from(user)); // includes role + dashboardPath for redirect

        } catch (DisabledException e) {
            return error(HttpStatus.UNAUTHORIZED, "This account has been disabled");
        } catch (AuthenticationException e) {
            // Same message for unknown email and wrong password, so accounts can't be enumerated
            return error(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
    }
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (req.role() == Role.ADMINISTRATOR) {
            return error(HttpStatus.FORBIDDEN, "This role cannot be selected during registration");
        }

        String email = req.email().trim().toLowerCase();
        if (users.findByEmailIgnoreCase(email).isPresent()) {
            return error(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = new User(req.fullName().trim(), email, encoder.encode(req.password()), req.role());
        try {
            users.save(user);
        } catch (DataIntegrityViolationException e) {
            return error(HttpStatus.CONFLICT, "Email is already registered");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    /**
     * Lets React restore login state on page refresh. 401 if there is no valid
     * session.
     */
    @GetMapping("/me")
    public UserResponse me(Authentication auth) {
        return UserResponse.from(users.findByEmailIgnoreCase(auth.getName()).orElseThrow());
    }

    @PutMapping("/profile")
    @Transactional
    public ResponseEntity<?> updateProfile(@Valid @RequestBody ProfileUpdateRequest req, Authentication auth,
            HttpServletRequest request, HttpServletResponse response) {
        User user = users.findByEmailIgnoreCase(auth.getName()).orElseThrow();
        String email = req.email().trim().toLowerCase();
        if (users.findByEmailIgnoreCase(email).filter(existing -> !existing.getId().equals(user.getId())).isPresent()) {
            return error(HttpStatus.CONFLICT, "Email is already registered");
        }

        boolean emailChanged = !user.getEmail().equalsIgnoreCase(email);
        user.setFullName(req.fullName().trim());
        user.setEmail(email);
        try {
            users.save(user);
        } catch (DataIntegrityViolationException e) {
            return error(HttpStatus.CONFLICT, "Email is already registered");
        }
        if (emailChanged) passwordResets.invalidateTokens(user);

        Authentication updatedAuth = UsernamePasswordAuthenticationToken.authenticated(
                user.getEmail(), auth.getCredentials(), auth.getAuthorities());
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(updatedAuth);
        contextRepo.saveContext(context, request, response);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PostMapping("/change-password")
    @Transactional
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest req, Authentication auth) {
        User user = users.findByEmailIgnoreCase(auth.getName()).orElseThrow();
        if (!encoder.matches(req.currentPassword(), user.getPassword())) {
            return error(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }
        if (!req.newPassword().equals(req.confirmPassword())) {
            return error(HttpStatus.BAD_REQUEST, "New passwords do not match");
        }
        if (encoder.matches(req.newPassword(), user.getPassword())) {
            return error(HttpStatus.BAD_REQUEST, "New password must be different from the current password");
        }

        user.setPassword(encoder.encode(req.newPassword()));
        users.save(user);
        passwordResets.invalidateTokens(user);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        try {
            passwordResets.requestReset(req.email());
        } catch (MailException e) {
            logger.error("Unable to deliver password reset email", e);
        }
        return ResponseEntity.accepted().body(Map.of("message",
                "If an account exists for that email, a password reset link will be sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        if (!req.newPassword().equals(req.confirmPassword())) {
            return error(HttpStatus.BAD_REQUEST, "New passwords do not match");
        }
        PasswordResetService.ResetResult result = passwordResets.resetPassword(req.token(), req.newPassword());
        if (result == PasswordResetService.ResetResult.INVALID_TOKEN) {
            return error(HttpStatus.BAD_REQUEST, "This reset link is invalid or has expired");
        }
        if (result == PasswordResetService.ResetResult.SAME_PASSWORD) {
            return error(HttpStatus.BAD_REQUEST, "New password must be different from the current password");
        }
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return error(HttpStatus.BAD_REQUEST, msg);
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String msg) {
        return ResponseEntity.status(status).body(Map.of("error", msg));
    }
    
}
