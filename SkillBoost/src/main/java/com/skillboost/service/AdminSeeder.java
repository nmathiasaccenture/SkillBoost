package com.skillboost.service;

import com.skillboost.model.AppUser;
import com.skillboost.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;
    private final String email;

    public AdminSeeder(UserRepository users,
                       PasswordEncoder encoder,
                       @Value("${skillboost.admin.username:admin}") String username,
                       @Value("${skillboost.admin.password:changeme}") String password,
                       @Value("${skillboost.admin.email:admin@skillboost.local}") String email) {
        this.users = users;
        this.encoder = encoder;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByRole(AppUser.Role.ADMIN)) {
            return;
        }
        if (users.existsByUsername(username)) {
            log.warn("Admin seed: username '{}' is already taken but no admin exists. "
                    + "Promote that user manually or change skillboost.admin.username.", username);
            return;
        }
        AppUser admin = new AppUser(username, email, encoder.encode(password), AppUser.Role.ADMIN);
        users.save(admin);
        log.info("Seeded initial admin user '{}'. Change the password after first login.", username);
    }
}
