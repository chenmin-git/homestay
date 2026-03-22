package com.homestay.repository;

import com.homestay.entity.PasswordResetRequest;
import com.homestay.enums.PasswordResetStatus;
import com.homestay.enums.RoleType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {

    boolean existsByUsernameAndRoleAndStatus(String username, RoleType role, PasswordResetStatus status);

    List<PasswordResetRequest> findAllByOrderByCreatedAtDesc();
}
