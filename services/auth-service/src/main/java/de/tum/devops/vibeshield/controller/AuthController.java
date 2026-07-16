package de.tum.devops.vibeshield.controller;

import de.tum.devops.vibeshield.dto.AuthResponse;
import de.tum.devops.vibeshield.dto.ErrorResponse;
import de.tum.devops.vibeshield.dto.ForgotPasswordRequest;
import de.tum.devops.vibeshield.dto.LoginRequest;
import de.tum.devops.vibeshield.dto.RegisterRequest;
import de.tum.devops.vibeshield.dto.ResetPasswordRequest;
import de.tum.devops.vibeshield.model.User;
import de.tum.devops.vibeshield.repository.UserRepository;
import de.tum.devops.vibeshield.service.JwtService;
import de.tum.devops.vibeshield.service.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for user authentication: registration, login, and current-user lookup.
 * All endpoints are served under {@code /api/v1/auth}.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** Injects the user store, JWT issuer, and password reset service used by these endpoints. */
    public AuthController(UserRepository userRepository, JwtService jwtService,
                           PasswordResetService passwordResetService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
    }

    /** Liveness check that confirms the auth service is reachable. */
    @GetMapping("/health")
    public String health() {
        return "Auth service running";
    }

    /** Registers a new user, rejecting duplicate emails and storing a BCrypt-hashed password. */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("EMAIL_ALREADY_REGISTERED", "Email is already registered"));
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User(
                request.getEmail(),
                hashedPassword
        );

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }

    /** Verifies email/password credentials and returns a signed JWT on success, 401 otherwise. */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(401)
                    .body(new ErrorResponse("INVALID_CREDENTIALS", "Invalid email or password"));
        }

        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        );

        if (!passwordMatches) {
            return ResponseEntity.status(401)
                    .body(new ErrorResponse("INVALID_CREDENTIALS", "Invalid email or password"));
        }

        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(new AuthResponse(token, user.getEmail()));
    }

    /**
     * Issues a password reset token if the email is registered. Always returns the same
     * response either way, so this endpoint cannot be used to enumerate accounts.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            userRepository.findByEmail(request.getEmail())
                    .ifPresent(passwordResetService::requestReset);
        } catch (MailException exception) {
            // Keep the public response indistinguishable from an unknown email while making
            // the delivery failure visible to operators. The transactional reset request is
            // rolled back, so an undelivered token is not left active.
            log.error("Could not deliver password reset email", exception);
        }

        return ResponseEntity.ok(Map.of("message",
                "If that email is registered, a password reset link has been sent."));
    }

    /** Exchanges a valid reset token for a new password, rejecting unknown/expired/used tokens. */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        boolean reset = passwordResetService.resetPassword(request.getToken(), request.getNewPassword());

        if (!reset) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_RESET_TOKEN", "Reset token is invalid, expired, or already used"));
        }

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    /** Returns the email of the user identified by the Bearer token in the Authorization header. */
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401)
                    .body(new ErrorResponse("UNAUTHORIZED", "Missing or invalid Authorization header"));
        }

        String token = authorizationHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.status(401)
                    .body(new ErrorResponse("INVALID_TOKEN", "Invalid token"));
        }

        String email = jwtService.extractEmail(token);

        return ResponseEntity.ok(Map.of("email", email));
    }
}
