package com.homestay.service;

import com.homestay.common.BusinessException;
import com.homestay.dto.AuthDtos.LoginRequest;
import com.homestay.dto.AuthDtos.RegisterRequest;
import com.homestay.entity.User;
import com.homestay.enums.RoleType;
import com.homestay.repository.UserRepository;
import com.homestay.security.JwtService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Map<String, Object> register(RegisterRequest request) {
        userRepository.findByUsername(request.username()).ifPresent(user -> {
            throw new BusinessException("用户名已存在");
        });
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setPhone(request.phone());
        user.setRole(RoleType.USER);
        userRepository.save(user);
        return tokenPayload(user);
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
}
