package com.homestay.repository;

import com.homestay.entity.Notice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findTop5ByPublishedTrueOrderByCreatedAtDesc();
}
