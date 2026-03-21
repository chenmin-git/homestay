package com.homestay.repository;

import com.homestay.entity.HostApplication;
import com.homestay.enums.HostApplyStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HostApplicationRepository extends JpaRepository<HostApplication, Long> {

    boolean existsByUsernameAndStatus(String username, HostApplyStatus status);

    boolean existsByPhoneAndStatus(String phone, HostApplyStatus status);

    Optional<HostApplication> findTopByUsernameOrderByCreatedAtDesc(String username);

    List<HostApplication> findAllByOrderByCreatedAtDesc();

    void deleteByUsername(String username);
}
