package de.tum.devops.vibeshield.controller;

import de.tum.devops.vibeshield.dto.AuthResponse;
import de.tum.devops.vibeshield.dto.LoginRequest;
import de.tum.devops.vibeshield.dto.RegisterRequest;
import de.tum.devops.vibeshield.model.User;
import de.tum.devops.vibeshield.repository.UserRepository;
import de.tum.devops.vibeshield.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @GetMapping("/health")
    public String health() {
        return "Auth service running";
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is already registered"));
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User(
                request.getEmail(),
                hashedPassword
        );

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }

        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        );

        if (!passwordMatches) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }

        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(new AuthResponse(token, user.getEmail()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
        }

        String token = authorizationHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        String email = jwtService.extractEmail(token);

        return ResponseEntity.ok(Map.of("email", email));
    }
}
