package de.tum.devops.vibeshield.repository;

import de.tum.devops.vibeshield.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}