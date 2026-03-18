package com.homestay.repository;

import com.homestay.entity.BookingOrder;
import com.homestay.entity.Homestay;
import com.homestay.entity.User;
import com.homestay.enums.OrderStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingOrderRepository extends JpaRepository<BookingOrder, Long> {
    List<BookingOrder> findByUserOrderByCreatedAtDesc(User user);

    List<BookingOrder> findByHomestayOrderByCreatedAtDesc(Homestay homestay);

    Optional<BookingOrder> findByOrderNo(String orderNo);

    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    @Query("select o from BookingOrder o " +
           "where o.orderStatus in :activeStatuses " +
           "  and o.id in ( " +
           "    select bor.order.id from BookingOrderRoom bor where bor.room.id in :roomIds " +
           "  ) " +
           "  and o.checkInDate < :checkOutDate " +
           "  and o.checkOutDate > :checkInDate")
    List<BookingOrder> findConflictingOrders(
        @Param("roomIds") List<Long> roomIds,
        @Param("checkInDate") LocalDate checkInDate,
        @Param("checkOutDate") LocalDate checkOutDate,
        @Param("activeStatuses") List<OrderStatus> activeStatuses
    );
}
