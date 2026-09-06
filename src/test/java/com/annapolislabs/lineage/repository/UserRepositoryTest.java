package com.annapolislabs.lineage.repository;

import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// The User entity maps its `preferences` column as Postgres `jsonb` (see User.java), which H2
// cannot emulate. Run this @DataJpaTest against a real Postgres container instead of the default
// embedded H2 database so schema generation and queries behave the same as production.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
// Close this test's Spring context (and its Hikari pool) immediately after this class finishes,
// before Testcontainers tears down the static Postgres container. Without this, the context stays
// open until JVM shutdown, by which point the container is already gone, causing Hikari to hang
// for 30s per connection trying to validate/close against a dead container.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgreSQL = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("lineage_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQL::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQL::getUsername);
        registry.add("spring.datasource.password", postgreSQL::getPassword);
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_UserExists_ReturnsUser() {
        // Arrange
        User user = new User("test@example.com", "hashedPassword", "Test User", UserRole.ADMINISTRATOR);
        entityManager.persistAndFlush(user);

        // Act
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("test@example.com", found.get().getEmail());
        assertEquals("Test User", found.get().getName());
    }

    @Test
    void findByEmail_UserDoesNotExist_ReturnsEmpty() {
        // Act
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void existsByEmail_UserExists_ReturnsTrue() {
        // Arrange
        User user = new User("test@example.com", "hashedPassword", "Test User", UserRole.ADMINISTRATOR);
        entityManager.persistAndFlush(user);

        // Act
        boolean exists = userRepository.existsByEmail("test@example.com");

        // Assert
        assertTrue(exists);
    }

    @Test
    void existsByEmail_UserDoesNotExist_ReturnsFalse() {
        // Act
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertFalse(exists);
    }

    @Test
    void save_NewUser_Persists() {
        // Arrange
        User user = new User("newuser@example.com", "hashedPassword", "New User", UserRole.DEVELOPER);

        // Act
        User saved = userRepository.save(user);
        entityManager.flush();

        // Assert
        assertNotNull(saved.getId());
        assertEquals("newuser@example.com", saved.getEmail());
    }
}
