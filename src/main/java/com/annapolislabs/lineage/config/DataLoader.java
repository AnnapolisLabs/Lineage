package com.annapolislabs.lineage.config;

import com.annapolislabs.lineage.entity.User;
import com.annapolislabs.lineage.entity.UserRole;
import com.annapolislabs.lineage.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        System.out.println("DataLoader: Checking for admin user...");

        String adminEmail = "admin@lineage.local";
        String rawPassword = "admin123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Find or create admin user
        User admin = userRepository.findByEmail(adminEmail).orElse(null);

        if (admin == null) {
            System.out.println("DataLoader: Creating new admin user");
            admin = new User(
                adminEmail,
                encodedPassword,
                "Admin User",
                UserRole.ADMIN
            );
        } else {
            System.out.println("DataLoader: Updating existing admin user password");
            admin.setPasswordHash(encodedPassword);
            admin.setRole(UserRole.ADMIN);
        }

        userRepository.save(admin);
        System.out.println("DataLoader: Admin user ready: admin@lineage.local / admin123");
    }
}
