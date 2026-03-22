package com.homestay.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public static class BookingCreateRequest {
        @NotNull private Long homestayId;
        @NotNull private LocalDate checkInDate;
        @NotNull private LocalDate checkOutDate;
        @NotEmpty private List<Long> roomIds;
        private String contactName;
        private String contactPhone;
        private String remark;

        public BookingCreateRequest() {}

        public Long getHomestayId() { return homestayId; }
        public LocalDate getCheckInDate() { return checkInDate; }
        public LocalDate getCheckOutDate() { return checkOutDate; }
        public List<Long> getRoomIds() { return roomIds; }
        public String getContactName() { return contactName; }
        public String getContactPhone() { return contactPhone; }
        public String getRemark() { return remark; }

        public void setHomestayId(Long homestayId) { this.homestayId = homestayId; }
        public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }
        public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }
        public void setRoomIds(List<Long> roomIds) { this.roomIds = roomIds; }
        public void setContactName(String contactName) { this.contactName = contactName; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    public static class ReviewCreateRequest {
        @NotNull private Long orderId;
        @NotNull private Integer score;
        private String content;
        private List<String> imageUrls;

        public ReviewCreateRequest() {}

        public Long getOrderId() { return orderId; }
        public Integer getScore() { return score; }
        public String getContent() { return content; }
        public List<String> getImageUrls() { return imageUrls; }

        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public void setScore(Integer score) { this.score = score; }
        public void setContent(String content) { this.content = content; }
        public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    }
}
