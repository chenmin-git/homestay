package com.homestay.config;

import com.homestay.entity.User;
import com.homestay.enums.RoleType;
import com.homestay.repository.UserRepository;
import com.homestay.service.AdminService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminService adminService;

    @Override
    public void run(String... args) {
        User admin = ensureUser("admin", "admin123", "系统管理员", RoleType.ADMIN, "13900000001");
        User host = ensureUser("host", "host123", "民宿房东", RoleType.HOST, "13900000002");
        ensureUser("user", "user123", "演示游客", RoleType.USER, "13900000003");
        adminService.seedIfEmpty(admin, host);
    }

    private User ensureUser(String username, String password, String nickname, RoleType role, String phone) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setNickname(nickname);
            user.setRole(role);
            user.setPhone(phone);
            return userRepository.save(user);
        });
    }
}
