package com.homestay.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public final class AuthDtos {

    private AuthDtos() {
    }

    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空") private String username;
        @NotBlank(message = "密码不能为空") private String password;

        public LoginRequest() {}

        public String getUsername() { return username; }
        public String getPassword() { return password; }

        public void setUsername(String username) { this.username = username; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空") private String username;
        @NotBlank(message = "密码不能为空") private String password;
        @NotBlank(message = "昵称不能为空") private String nickname;
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;

        public RegisterRequest() {}

        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getNickname() { return nickname; }
        public String getPhone() { return phone; }

        public void setUsername(String username) { this.username = username; }
        public void setPassword(String password) { this.password = password; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    public static class HostApplyRequest {
        @NotBlank(message = "用户名不能为空") private String username;
        @NotBlank(message = "密码不能为空") private String password;
        @NotBlank(message = "昵称不能为空") private String nickname;
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;

        public HostApplyRequest() {}

        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getNickname() { return nickname; }
        public String getPhone() { return phone; }

        public void setUsername(String username) { this.username = username; }
        public void setPassword(String password) { this.password = password; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    public static class UserPasswordResetRequest {
        @NotBlank(message = "用户名不能为空") private String username;
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;
        @NotBlank(message = "新密码不能为空") private String newPassword;

        public UserPasswordResetRequest() {}

        public String getUsername() { return username; }
        public String getPhone() { return phone; }
        public String getNewPassword() { return newPassword; }

        public void setUsername(String username) { this.username = username; }
        public void setPhone(String phone) { this.phone = phone; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class HostPasswordResetRequest {
        @NotBlank(message = "用户名不能为空") private String username;
        @NotBlank(message = "昵称不能为空") private String nickname;
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;
        @NotBlank(message = "新密码不能为空") private String newPassword;

        public HostPasswordResetRequest() {}

        public String getUsername() { return username; }
        public String getNickname() { return nickname; }
        public String getPhone() { return phone; }
        public String getNewPassword() { return newPassword; }

        public void setUsername(String username) { this.username = username; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public void setPhone(String phone) { this.phone = phone; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}
