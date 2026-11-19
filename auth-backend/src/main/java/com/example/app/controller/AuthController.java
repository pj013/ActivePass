package com.example.app.controller;

import java.util.Map;

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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.dto.LoginRequest;
import com.example.app.dto.RegisterRequest;
import com.example.app.dto.UserResponse;
import com.example.app.model.Role;
import com.example.app.model.User;
import com.example.app.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final SecurityContextRepository contextRepo;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthController(AuthenticationManager authManager, SecurityContextRepository contextRepo,
            UserRepository users, PasswordEncoder encoder) {
        this.authManager = authManager;
        this.contextRepo = contextRepo;
        this.users = users;
        this.encoder = encoder;  
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return error(HttpStatus.BAD_REQUEST, msg);
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String msg) {
        return ResponseEntity.status(status).body(Map.of("error", msg));
    }
    
}
