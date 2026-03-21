package com.homestay.dto;

import jakarta.validation.constraints.NotBlank;

public final class UserDtos {

    private UserDtos() {
    }

    public record ProfileUpdateRequest(
        @NotBlank(message = "昵称不能为空") String nickname,
        String avatar,
        String phone
    ) {
    }

    public record PasswordChangeRequest(
        @NotBlank(message = "原密码不能为空") String oldPassword,
        @NotBlank(message = "新密码不能为空") String newPassword
    ) {
    }
}
