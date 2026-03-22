package com.homestay.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    public static class RoomForm {
        private Long id;
        @NotBlank private String roomNo;
        @NotBlank private String roomType;
        @NotNull private Integer floorNo;
        @NotNull private BigDecimal price;
        @NotNull private Integer bedCount;
        @NotNull private Integer capacity;

        public RoomForm() {}

        public RoomForm(Long id, String roomNo, String roomType, Integer floorNo, BigDecimal price, Integer bedCount, Integer capacity) {
            this.id = id;
            this.roomNo = roomNo;
            this.roomType = roomType;
            this.floorNo = floorNo;
            this.price = price;
            this.bedCount = bedCount;
            this.capacity = capacity;
        }

        public Long getId() { return id; }
        public String getRoomNo() { return roomNo; }
        public String getRoomType() { return roomType; }
        public Integer getFloorNo() { return floorNo; }
        public BigDecimal getPrice() { return price; }
        public Integer getBedCount() { return bedCount; }
        public Integer getCapacity() { return capacity; }

        public void setId(Long id) { this.id = id; }
        public void setRoomNo(String roomNo) { this.roomNo = roomNo; }
        public void setRoomType(String roomType) { this.roomType = roomType; }
        public void setFloorNo(Integer floorNo) { this.floorNo = floorNo; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public void setBedCount(Integer bedCount) { this.bedCount = bedCount; }
        public void setCapacity(Integer capacity) { this.capacity = capacity; }
    }

    public static class HomestaySaveRequest {
        @NotBlank private String name;
        @NotBlank private String city;
        private String district;
        @NotBlank private String address;
        @NotNull private BigDecimal basePrice;
        @NotBlank private String houseType;
        @NotBlank private String tags;
        @NotBlank private String facilities;
        @NotNull private Double latitude;
        @NotNull private Double longitude;
        @NotBlank private String coverImage;
        @NotBlank private String summary;
        @NotBlank private String description;
        @NotEmpty private List<String> images;
        @NotEmpty private List<RoomForm> rooms;

        public HomestaySaveRequest() {}

        public String getName() { return name; }
        public String getCity() { return city; }
        public String getDistrict() { return district; }
        public String getAddress() { return address; }
        public BigDecimal getBasePrice() { return basePrice; }
        public String getHouseType() { return houseType; }
        public String getTags() { return tags; }
        public String getFacilities() { return facilities; }
        public Double getLatitude() { return latitude; }
        public Double getLongitude() { return longitude; }
        public String getCoverImage() { return coverImage; }
        public String getSummary() { return summary; }
        public String getDescription() { return description; }
        public List<String> getImages() { return images; }
        public List<RoomForm> getRooms() { return rooms; }

        public void setName(String name) { this.name = name; }
        public void setCity(String city) { this.city = city; }
        public void setDistrict(String district) { this.district = district; }
        public void setAddress(String address) { this.address = address; }
        public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
        public void setHouseType(String houseType) { this.houseType = houseType; }
        public void setTags(String tags) { this.tags = tags; }
        public void setFacilities(String facilities) { this.facilities = facilities; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
        public void setSummary(String summary) { this.summary = summary; }
        public void setDescription(String description) { this.description = description; }
        public void setImages(List<String> images) { this.images = images; }
        public void setRooms(List<RoomForm> rooms) { this.rooms = rooms; }
    }

    public static class ReviewReplyRequest {
        @NotBlank private String replyContent;
        public ReviewReplyRequest() {}
        public String getReplyContent() { return replyContent; }
        public void setReplyContent(String replyContent) { this.replyContent = replyContent; }
    }

    public static class BannerSaveRequest {
        private String title;
        @NotBlank private String imageUrl;
        private String linkUrl;
        private Integer sortOrder;
        private Boolean enabled;

        public BannerSaveRequest() {}

        public String getTitle() { return title; }
        public String getImageUrl() { return imageUrl; }
        public String getLinkUrl() { return linkUrl; }
        public Integer getSortOrder() { return sortOrder; }
        public Boolean getEnabled() { return enabled; }

        public void setTitle(String title) { this.title = title; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }

    public static class NoticeSaveRequest {
        @NotBlank private String title;
        @NotBlank private String content;
        private Boolean published;

        public NoticeSaveRequest() {}

        public String getTitle() { return title; }
        public String getContent() { return content; }
        public Boolean getPublished() { return published; }

        public void setTitle(String title) { this.title = title; }
        public void setContent(String content) { this.content = content; }
        public void setPublished(Boolean published) { this.published = published; }
    }

    public static class PasswordChangeRequest {
        @NotBlank private String oldPassword;
        @NotBlank private String newPassword;

        public PasswordChangeRequest() {}

        public String getOldPassword() { return oldPassword; }
        public String getNewPassword() { return newPassword; }

        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}
