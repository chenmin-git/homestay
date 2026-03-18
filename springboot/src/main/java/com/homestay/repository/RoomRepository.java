package com.homestay.repository;

import com.homestay.entity.Homestay;
import com.homestay.entity.Room;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByHomestayAndEnabledTrueOrderByRoomNoAsc(Homestay homestay);
}
