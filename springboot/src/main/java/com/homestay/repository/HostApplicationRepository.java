package com.homestay.repository;

import com.homestay.entity.HostApplication;
import com.homestay.enums.HostApplyStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HostApplicationRepository extends JpaRepository<HostApplication, Long> {

    boolean existsByUsernameAndStatus(String username, HostApplyStatus status);

    List<HostApplication> findAllByOrderByCreatedAtDesc();
}
