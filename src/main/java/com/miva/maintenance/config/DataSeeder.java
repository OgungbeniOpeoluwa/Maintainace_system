package com.miva.maintenance.config;

import com.miva.maintenance.model.RequestCategory;
import com.miva.maintenance.model.Role;
import com.miva.maintenance.model.User;
import com.miva.maintenance.repository.RequestCategoryRepository;
import com.miva.maintenance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds a default admin account and starter request categories on first run.
 * Default admin: admin@miva.university / Admin@123 (CHANGE THIS AFTER FIRST LOGIN)
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RequestCategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Check specifically for the admin account, not "any user at all" — otherwise a single
        // stray non-admin record (leftover test data, a race with an early registration, etc.)
        // would silently and permanently prevent the admin from ever being seeded.
        if (userRepository.findByEmail("admin@miva.university").isEmpty()) {
            userRepository.save(User.builder()
                    .fullName("System Administrator")
                    .email("admin@miva.university")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .active(true)
                    .build());
            System.out.println(">>> Seeded default admin: admin@miva.university / Admin@123");
        }

        if (categoryRepository.count() == 0) {
            categoryRepository.saveAll(List.of(
                    RequestCategory.builder().name("Electrical").description("Faulty electricity, sockets, lighting").build(),
                    RequestCategory.builder().name("Plumbing").description("Leaking pipes, taps, drainage").build(),
                    RequestCategory.builder().name("Furniture").description("Damaged furniture / fittings").build(),
                    RequestCategory.builder().name("Internet").description("Network / Wi-Fi issues").build(),
                    RequestCategory.builder().name("Classroom Equipment").description("Projectors, boards, ACs").build(),
                    RequestCategory.builder().name("Hostel Maintenance").description("General hostel complaints").build()
            ));
            System.out.println(">>> Seeded default request categories");
        }
    }
}