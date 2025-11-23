package com.annapolislabs.lineage.repository;

import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.flyway.enabled=false"
})
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Disabled("Database compatibility issue - H2 vs PostgreSQL")
    void findByEmail_UserExists_ReturnsUser() {
        // Arrange
        User user = new User("test@example.com", "hashedPassword", "Test User", UserRole.ADMIN);
        entityManager.persistAndFlush(user);

        // Act
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("test@example.com", found.get().getEmail());
        assertEquals("Test User", found.get().getName());
    }

    @Test
    @Disabled("Database compatibility issue - H2 vs PostgreSQL")
    void findByEmail_UserDoesNotExist_ReturnsEmpty() {
        // Act
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    @Disabled("Database compatibility issue - H2 vs PostgreSQL")
    void existsByEmail_UserExists_ReturnsTrue() {
        // Arrange
        User user = new User("test@example.com", "hashedPassword", "Test User", UserRole.ADMIN);
        entityManager.persistAndFlush(user);

        // Act
        boolean exists = userRepository.existsByEmail("test@example.com");

        // Assert
        assertTrue(exists);
    }

    @Test
    @Disabled("Database compatibility issue - H2 vs PostgreSQL")
    void existsByEmail_UserDoesNotExist_ReturnsFalse() {
        // Act
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertFalse(exists);
    }

    @Test
    @Disabled("Database compatibility issue - H2 vs PostgreSQL")
    void save_NewUser_Persists() {
        // Arrange
        User user = new User("newuser@example.com", "hashedPassword", "New User", UserRole.EDITOR);

        // Act
        User saved = userRepository.save(user);
        entityManager.flush();

        // Assert
        assertNotNull(saved.getId());
        assertEquals("newuser@example.com", saved.getEmail());
        assertEquals(UserRole.EDITOR, saved.getRole());
    }
}
