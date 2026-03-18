package com.homestay.repository;

import com.homestay.entity.Homestay;
import com.homestay.entity.User;
import com.homestay.enums.HomestayStatus;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HomestayRepository extends JpaRepository<Homestay, Long> {

    @Query("select h from Homestay h " +
           "where h.status = :status " +
           "  and (:city is null or h.city like concat('%', :city, '%') or h.district like concat('%', :city, '%')) " +
           "  and (:keyword is null or h.name like concat('%', :keyword, '%') or h.summary like concat('%', :keyword, '%')) " +
           "  and (:minPrice is null or h.basePrice >= :minPrice) " +
           "  and (:maxPrice is null or h.basePrice <= :maxPrice) " +
           "  and (:houseType is null or h.houseType = :houseType) " +
           "order by h.recommended desc, h.bookingCount desc, h.createdAt desc")
    Page<Homestay> search(
        @Param("status") HomestayStatus status,
        @Param("city") String city,
        @Param("keyword") String keyword,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("houseType") String houseType,
        Pageable pageable
    );

    @Query("select h from Homestay h " +
           "where h.status = :status " +
           "  and (:city is null or h.city like concat('%', :city, '%') or h.district like concat('%', :city, '%')) " +
           "  and (:keyword is null or h.name like concat('%', :keyword, '%') or h.summary like concat('%', :keyword, '%')) " +
           "  and (:minPrice is null or h.basePrice >= :minPrice) " +
           "  and (:maxPrice is null or h.basePrice <= :maxPrice) " +
           "  and (:houseType is null or h.houseType = :houseType) " +
           "order by h.recommended desc, h.bookingCount desc, h.createdAt desc")
    List<Homestay> searchAll(
        @Param("status") HomestayStatus status,
        @Param("city") String city,
        @Param("keyword") String keyword,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("houseType") String houseType
    );

    List<Homestay> findTop6ByStatusOrderByBookingCountDescCreatedAtDesc(HomestayStatus status);

    List<Homestay> findTop6ByStatusOrderByCreatedAtDesc(HomestayStatus status);

    List<Homestay> findByHost(User host);
}
