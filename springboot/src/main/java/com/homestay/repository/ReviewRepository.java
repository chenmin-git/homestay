package com.homestay.repository;

import com.homestay.entity.BookingOrder;
import com.homestay.entity.Homestay;
import com.homestay.entity.Review;
import com.homestay.enums.ReviewStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByHomestayAndStatusOrderByCreatedAtDesc(Homestay homestay, ReviewStatus status);

    List<Review> findByHomestayOrderByCreatedAtDesc(Homestay homestay);

    List<Review> findAllByOrderByCreatedAtDesc();

    Optional<Review> findByOrder(BookingOrder order);
}
