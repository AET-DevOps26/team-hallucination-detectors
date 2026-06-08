package de.tum.devops.vibeshield.integration;

import de.tum.devops.vibeshield.model.User;
import de.tum.devops.vibeshield.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for the persistence layer: the real {@link UserRepository} against a
 * full application context backed by an in-memory H2 database. Verifies the JPA mapping,
 * the email lookup, and the unique-email constraint that a mocked repository can't prove.
 */
@SpringBootTest
@ActiveProfiles("test")
class UserPersistenceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clean() {
        userRepository.deleteAll();
    }

    // I1
    @Test
    void findByEmail_returnsPersistedUser() {
        userRepository.save(new User("persist@example.com", "hash"));

        Optional<User> found = userRepository.findByEmail("persist@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("persist@example.com");
        assertThat(found.get().getId()).isNotNull();
    }

    // I2
    @Test
    void findByEmail_unknownAddress_returnsEmpty() {
        assertThat(userRepository.findByEmail("missing@example.com")).isEmpty();
    }

    // I3
    @Test
    void save_duplicateEmail_violatesUniqueConstraint() {
        userRepository.saveAndFlush(new User("dupe@example.com", "hash-1"));

        assertThatThrownBy(() ->
                userRepository.saveAndFlush(new User("dupe@example.com", "hash-2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
