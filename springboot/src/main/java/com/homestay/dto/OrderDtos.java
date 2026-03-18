package com.homestay.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record BookingCreateRequest(
        @NotNull Long homestayId,
        @NotNull LocalDate checkInDate,
        @NotNull LocalDate checkOutDate,
        @NotEmpty List<Long> roomIds,
        String contactName,
        String contactPhone,
        String remark
    ) {
    }

    public record ReviewCreateRequest(
        @NotNull Long orderId,
        @NotNull Integer score,
        String content,
        List<String> imageUrls
    ) {
    }
}
