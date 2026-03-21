package com.homestay.service;

import com.homestay.common.BusinessException;
import com.homestay.dto.AuthDtos.HostPasswordResetRequest;
import com.homestay.dto.AuthDtos.HostApplyRequest;
import com.homestay.dto.AuthDtos.LoginRequest;
import com.homestay.dto.AuthDtos.RegisterRequest;
import com.homestay.dto.AuthDtos.UserPasswordResetRequest;
import com.homestay.entity.HostApplication;
import com.homestay.entity.PasswordResetRequest;
import com.homestay.entity.User;
import com.homestay.enums.HostApplyStatus;
import com.homestay.enums.PasswordResetStatus;
import com.homestay.enums.RoleType;
import com.homestay.repository.HostApplicationRepository;
import com.homestay.repository.PasswordResetRequestRepository;
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
    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        UserRepository userRepository,
        HostApplicationRepository hostApplicationRepository,
        PasswordResetRequestRepository passwordResetRequestRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.hostApplicationRepository = hostApplicationRepository;
        this.passwordResetRequestRepository = passwordResetRequestRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Map<String, Object> register(RegisterRequest request) {
        userRepository.findByUsername(request.username()).ifPresent(user -> {
            throw new BusinessException("用户名已存在");
        });
        if (request.phone() != null && userRepository.existsByPhone(request.phone())) {
            throw new BusinessException("该手机号已注册");
        }
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
        if (request.phone() != null) {
            if (userRepository.existsByPhone(request.phone())) {
                throw new BusinessException("该手机号已注册");
            }
            if (hostApplicationRepository.existsByPhoneAndStatus(request.phone(), HostApplyStatus.PENDING)) {
                throw new BusinessException("该手机号已有待审核申请");
            }
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
            .orElseGet(() -> {
                hostApplicationRepository.findTopByUsernameOrderByCreatedAtDesc(request.username()).ifPresent(application -> {
                    if (application.getStatus() == HostApplyStatus.PENDING) {
                        throw new BusinessException("房东申请待管理员审核，审核通过后才能登录");
                    }
                    if (application.getStatus() == HostApplyStatus.REJECTED) {
                        throw new BusinessException("房东申请未通过，请重新提交申请");
                    }
                });
                throw new BusinessException("账号或密码错误");
            });
        if (!user.getEnabled()) {
            throw new BusinessException("账号已被禁用");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        return tokenPayload(user);
    }

    public Map<String, Object> resetUserPassword(UserPasswordResetRequest request) {
        User user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new BusinessException("账号信息不匹配"));
        if (user.getRole() != RoleType.USER) {
            throw new BusinessException("该账号不是普通用户账号");
        }
        if (!user.getEnabled()) {
            throw new BusinessException("账号已被禁用，无法重置密码");
        }
        if (!request.phone().equals(user.getPhone())) {
            throw new BusinessException("账号信息不匹配");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        return tokenPayload(user);
    }

    public Map<String, Object> submitHostPasswordReset(HostPasswordResetRequest request) {
        User user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new BusinessException("房东账号不存在"));
        if (user.getRole() != RoleType.HOST) {
            throw new BusinessException("该账号不是房东账号");
        }
        if (!request.phone().equals(user.getPhone()) || !request.nickname().equals(user.getNickname())) {
            throw new BusinessException("提交的信息与房东账号不匹配");
        }
        if (passwordResetRequestRepository.existsByUsernameAndRoleAndStatus(
            user.getUsername(),
            RoleType.HOST,
            PasswordResetStatus.PENDING
        )) {
            throw new BusinessException("已存在待审核的改密申请，请耐心等待管理员处理");
        }

        PasswordResetRequest resetRequest = new PasswordResetRequest();
        resetRequest.setUsername(user.getUsername());
        resetRequest.setNickname(user.getNickname());
        resetRequest.setPhone(user.getPhone());
        resetRequest.setRole(RoleType.HOST);
        resetRequest.setNewPassword(passwordEncoder.encode(request.newPassword()));
        resetRequest.setStatus(PasswordResetStatus.PENDING);
        passwordResetRequestRepository.save(resetRequest);
        return Map.of("id", resetRequest.getId(), "status", resetRequest.getStatus().name());
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
            "role", user.getRole().name(),
            "blacklisted", Boolean.TRUE.equals(user.getBlacklisted())
        ));
        return data;
    }

    private void validatePhone(String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("手机号格式不正确");
        }
    }
}
