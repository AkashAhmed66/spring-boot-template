package com.template.springboot.config;

import com.template.springboot.modules.role.entity.Role;
import com.template.springboot.modules.role.enums.RoleName;
import com.template.springboot.modules.role.repository.RoleRepository;
import com.template.springboot.modules.user.entity.User;
import com.template.springboot.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminEmail;
    private final String adminPassword;

    DataInitializer(UserRepository userRepository,
                    RoleRepository roleRepository,
                    PasswordEncoder passwordEncoder,
                    @Value("${app.bootstrap.admin.username:admin}") String adminUsername,
                    @Value("${app.bootstrap.admin.email:admin@gmail.com}") String adminEmail,
                    @Value("${app.bootstrap.admin.password:admin123}") String adminPassword) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByUsername(adminUsername)) {
            return;
        }
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException(RoleName.ADMIN + " role missing — check Flyway migrations"));

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setFirstName("System");
        admin.setLastName("Administrator");
        admin.setEnabled(true);
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        admin.setRoles(roles);
        userRepository.save(admin);

        log.warn("Bootstrapped default admin user '{}'. CHANGE THE PASSWORD before going to production.", adminUsername);
    }
}
