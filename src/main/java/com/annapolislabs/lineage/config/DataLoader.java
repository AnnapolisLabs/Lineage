package com.annapolislabs.lineage.config;

import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${lineage.admin.email:}")
    private String adminEmail;

    @Value("${lineage.admin.password:}")
    private String adminPassword;

    public DataLoader(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        logger.info("DataLoader: Checking for admin user configuration...");

        // Skip DataLoader if admin credentials are not configured
        if (adminEmail == null || adminEmail.trim().isEmpty() || adminPassword == null || adminPassword.trim().isEmpty()) {
            logger.warn("DataLoader: Admin credentials not configured via environment variables. Skipping admin user creation.");
            logger.warn("DataLoader: Please set LINEAGE_ADMIN_EMAIL and LINEAGE_ADMIN_PASSWORD environment variables.");
            return;
        }

        String encodedPassword = passwordEncoder.encode(adminPassword);

        // Find or create admin user
        User admin = userRepository.findByEmail(adminEmail).orElse(null);

        if (admin == null) {
            logger.info("DataLoader: Creating new admin user");
            admin = new User(
                adminEmail,
                encodedPassword,
                "Admin User",
                UserRole.ADMIN
            );
        } else {
            logger.info("DataLoader: Updating existing admin user password");
            admin.setPasswordHash(encodedPassword);
            admin.setRole(UserRole.ADMIN);
        }

        userRepository.save(admin);
        logger.info("DataLoader: Admin user ready");
    }
}
