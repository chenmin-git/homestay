package com.homestay.repository;

import com.homestay.entity.Favorite;
import com.homestay.entity.Homestay;
import com.homestay.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserAndHomestay(User user, Homestay homestay);

    List<Favorite> findByUserOrderByCreatedAtDesc(User user);

    List<Favorite> findByHomestay(Homestay homestay);

    long countByHomestay(Homestay homestay);

    void deleteByUser(User user);
}
