package de.tum.devops.vibeshield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// Scheduling drives the background scan worker (see scanner/ScanWorker).
@EnableScheduling
@SpringBootApplication
public class VibeShieldApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(VibeShieldApiApplication.class, args);
    }
}
