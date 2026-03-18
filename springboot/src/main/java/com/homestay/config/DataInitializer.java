package com.homestay.config;

import com.homestay.entity.User;
import com.homestay.enums.RoleType;
import com.homestay.repository.UserRepository;
import com.homestay.service.AdminService;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminService adminService;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder, AdminService adminService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminService = adminService;
    }

    @Override
    public void run(String... args) {
        User admin = ensureUser("admin", "admin123", "系统管理员", RoleType.ADMIN, "13900000001");
        User host = ensureUser("host", "host123", "民宿房东", RoleType.HOST, "13900000002");
        ensureUser("user", "user123", "演示游客", RoleType.USER, "13900000003");
        ensureUser("user2", "user123", "差旅白领", RoleType.USER, "13900000004");
        ensureUser("user3", "user123", "周末旅行家", RoleType.USER, "13900000005");
        ensureUser("host2", "host123", "城市房东", RoleType.HOST, "13900000008");
        ensureUserFlags(ensureUser("blocked", "blocked123", "黑名单演示", RoleType.USER, "13900000006"), true, true);
        ensureUserFlags(ensureUser("disabled", "disabled123", "禁用演示", RoleType.USER, "13900000007"), false, false);
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

    private void ensureUserFlags(User user, boolean enabled, boolean blacklisted) {
        boolean changed = false;
        if (!Boolean.valueOf(enabled).equals(user.getEnabled())) {
            user.setEnabled(enabled);
            changed = true;
        }
        if (!Boolean.valueOf(blacklisted).equals(user.getBlacklisted())) {
            user.setBlacklisted(blacklisted);
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
        }
    }
}
