package de.tum.devops.vibeshield.controller;

import de.tum.devops.vibeshield.dto.RegisterRequest;
import de.tum.devops.vibeshield.model.User;
import de.tum.devops.vibeshield.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

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
}