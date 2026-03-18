package com.homestay.repository;

import com.homestay.entity.Banner;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BannerRepository extends JpaRepository<Banner, Long> {
    List<Banner> findByEnabledTrueOrderBySortOrderAsc();
}
