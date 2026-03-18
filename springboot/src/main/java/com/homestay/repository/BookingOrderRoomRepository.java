package com.homestay.repository;

import com.homestay.entity.BookingOrder;
import com.homestay.entity.BookingOrderRoom;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingOrderRoomRepository extends JpaRepository<BookingOrderRoom, Long> {
    List<BookingOrderRoom> findByOrder(BookingOrder order);
}
