package com.homestay.repository;

import com.homestay.entity.Homestay;
import com.homestay.entity.HomestayImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomestayImageRepository extends JpaRepository<HomestayImage, Long> {
    List<HomestayImage> findByHomestayOrderBySortOrderAsc(Homestay homestay);
}
