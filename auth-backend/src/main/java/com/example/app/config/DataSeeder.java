package com.example.app.config;

import com.example.app.model.Role;
import com.example.app.model.User;
import com.example.app.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** DEV ONLY: creates one test account per role. Remove once registration exists. */
@Configuration
public class DataSeeder {
    @Bean
    CommandLineRunner seed(UserRepository repo, PasswordEncoder enc) {
        return args -> {
            if (repo.count() > 0) return;
            repo.save(new User("Test Student",   "student@test.com",   enc.encode("Student123!"), Role.STUDENT));
            repo.save(new User("Test Organizer", "organizer@test.com", enc.encode("Organizer123!"), Role.ORGANIZER));
            repo.save(new User("Test Admin",     "admin@test.com",     enc.encode("Admin123!"), Role.ADMINISTRATOR));
        };
    }
}
