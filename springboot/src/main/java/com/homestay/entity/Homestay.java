package com.homestay.entity;

import com.homestay.enums.HomestayStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "homestay")
public class Homestay extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String city;

    @Column(length = 255)
    private String district;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal basePrice;

    @Column(nullable = false)
    private Integer totalRooms = 0;

    @Column(nullable = false, length = 50)
    private String houseType;

    @Column(nullable = false, length = 255)
    private String tags;

    @Column(nullable = false, length = 255)
    private String facilities;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Integer favoriteCount = 0;

    @Column(nullable = false)
    private Integer bookingCount = 0;

    @Column(nullable = false)
    private Double rating = 0D;

    @Column(nullable = false)
    private Boolean recommended = false;

    @Column(nullable = false)
    private Boolean latestListed = true;

    @Column(nullable = false, length = 500)
    private String coverImage;

    @Column(nullable = false, length = 1000)
    private String summary;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HomestayStatus status = HomestayStatus.ONLINE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;
}
