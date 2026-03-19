package com.homestay.service;

import com.homestay.common.BusinessException;
import com.homestay.dto.AuthDtos.HostApplyRequest;
import com.homestay.dto.AuthDtos.LoginRequest;
import com.homestay.dto.AuthDtos.RegisterRequest;
import com.homestay.entity.HostApplication;
import com.homestay.entity.User;
import com.homestay.enums.HostApplyStatus;
import com.homestay.enums.RoleType;
import com.homestay.repository.HostApplicationRepository;
import com.homestay.repository.UserRepository;
import com.homestay.security.JwtService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final HostApplicationRepository hostApplicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        UserRepository userRepository,
        HostApplicationRepository hostApplicationRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.hostApplicationRepository = hostApplicationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Map<String, Object> register(RegisterRequest request) {
        userRepository.findByUsername(request.username()).ifPresent(user -> {
            throw new BusinessException("用户名已存在");
        });
        validatePhone(request.phone());
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setPhone(request.phone());
        user.setRole(RoleType.USER);
        userRepository.save(user);
        return tokenPayload(user);
    }

    public Map<String, Object> applyHost(HostApplyRequest request) {
        userRepository.findByUsername(request.username()).ifPresent(user -> {
            throw new BusinessException("用户名已存在");
        });
        if (hostApplicationRepository.existsByUsernameAndStatus(request.username(), HostApplyStatus.PENDING)) {
            throw new BusinessException("申请已提交，请耐心等待审核");
        }
        validatePhone(request.phone());
        HostApplication application = new HostApplication();
        application.setUsername(request.username());
        application.setPassword(passwordEncoder.encode(request.password()));
        application.setNickname(request.nickname());
        application.setPhone(request.phone());
        application.setStatus(HostApplyStatus.PENDING);
        hostApplicationRepository.save(application);
        return Map.of("id", application.getId(), "status", application.getStatus().name());
    }

    public Map<String, Object> login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new BusinessException("账号或密码错误"));
        if (!user.getEnabled()) {
            throw new BusinessException("账号已被禁用");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        return tokenPayload(user);
    }

    public Map<String, Object> tokenPayload(User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", jwtService.generateToken(user));
        data.put("user", Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "nickname", user.getNickname(),
            "phone", user.getPhone() == null ? "" : user.getPhone(),
            "avatar", user.getAvatar() == null ? "" : user.getAvatar(),
            "role", user.getRole().name()
        ));
        return data;
    }

    private void validatePhone(String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("手机号格式不正确");
        }
    }
}
