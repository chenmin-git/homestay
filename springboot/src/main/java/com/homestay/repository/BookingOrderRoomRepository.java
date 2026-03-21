package com.homestay.repository;

import com.homestay.entity.BookingOrder;
import com.homestay.entity.BookingOrderRoom;
import com.homestay.entity.Room;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingOrderRoomRepository extends JpaRepository<BookingOrderRoom, Long> {
    List<BookingOrderRoom> findByOrder(BookingOrder order);

    List<BookingOrderRoom> findByOrderIn(List<BookingOrder> orders);

    boolean existsByRoom(Room room);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("DELETE FROM BookingOrderRoom b WHERE b.order = ?1")
    void deleteByOrder(BookingOrder order);
}
