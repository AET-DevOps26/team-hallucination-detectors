package de.tum.devops.vibeshield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the VibeShield authentication service.
 */
@SpringBootApplication
public class AuthServiceApplication {

    /** Boots the Spring application context and starts the embedded web server. */
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
