package de.tum.devops.vibeshield.controller;

import de.tum.devops.vibeshield.dto.RegisterRequest;
import de.tum.devops.vibeshield.model.User;
import de.tum.devops.vibeshield.repository.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import de.tum.devops.vibeshield.dto.LoginRequest;
import org.springframework.http.ResponseEntity;
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/health")
    public String health() {
        return "Auth service running";
    }

    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest request) {
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User(
                request.getEmail(),
                hashedPassword
        );

        userRepository.save(user);

        return "User registered successfully";
    }
   @PostMapping("/login")
public ResponseEntity<String> login(@RequestBody LoginRequest request) {
    User user = userRepository.findByEmail(request.getEmail())
            .orElse(null);

    if (user == null) {
        return ResponseEntity.status(401).body("Invalid email or password");
    }

    boolean passwordMatches = passwordEncoder.matches(
            request.getPassword(),
            user.getPassword()
    );

    if (!passwordMatches) {
        return ResponseEntity.status(401).body("Invalid email or password");
    }

    return ResponseEntity.ok("Login successful");
}
}