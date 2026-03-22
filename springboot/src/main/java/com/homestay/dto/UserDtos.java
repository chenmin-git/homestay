package com.homestay.dto;

import javax.validation.constraints.NotBlank;

public final class UserDtos {

    private UserDtos() {
    }

    public static class ProfileUpdateRequest {
        @NotBlank(message = "昵称不能为空") private String nickname;
        private String avatar;
        private String phone;

        public ProfileUpdateRequest() {}

        public String getNickname() { return nickname; }
        public String getAvatar() { return avatar; }
        public String getPhone() { return phone; }

        public void setNickname(String nickname) { this.nickname = nickname; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    public static class PasswordChangeRequest {
        @NotBlank(message = "原密码不能为空") private String oldPassword;
        @NotBlank(message = "新密码不能为空") private String newPassword;

        public PasswordChangeRequest() {}

        public String getOldPassword() { return oldPassword; }
        public String getNewPassword() { return newPassword; }

        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}
