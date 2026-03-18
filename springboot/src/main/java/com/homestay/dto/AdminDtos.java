package com.homestay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record RoomForm(
        @NotBlank String roomNo,
        @NotBlank String roomType,
        @NotNull Integer floorNo,
        @NotNull BigDecimal price,
        @NotNull Integer bedCount,
        @NotNull Integer capacity
    ) {
    }

    public record HomestaySaveRequest(
        @NotBlank String name,
        @NotBlank String city,
        String district,
        @NotBlank String address,
        @NotNull BigDecimal basePrice,
        @NotBlank String houseType,
        @NotBlank String tags,
        @NotBlank String facilities,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotBlank String coverImage,
        @NotBlank String summary,
        @NotBlank String description,
        @NotEmpty List<String> images,
        @NotEmpty List<RoomForm> rooms
    ) {
    }

    public record ReviewReplyRequest(@NotBlank String replyContent) {
    }
}
