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
}
