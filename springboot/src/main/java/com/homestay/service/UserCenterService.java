package com.homestay.service;

import com.homestay.common.BusinessException;
import com.homestay.dto.OrderDtos.BookingCreateRequest;
import com.homestay.dto.OrderDtos.ReviewCreateRequest;
import com.homestay.dto.UserDtos.ProfileUpdateRequest;
import com.homestay.entity.BookingOrder;
import com.homestay.entity.BookingOrderRoom;
import com.homestay.entity.Favorite;
import com.homestay.entity.Homestay;
import com.homestay.entity.Review;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.enums.OrderStatus;
import com.homestay.enums.PaymentStatus;
import com.homestay.enums.ReviewStatus;
import com.homestay.repository.BookingOrderRepository;
import com.homestay.repository.BookingOrderRoomRepository;
import com.homestay.repository.FavoriteRepository;
import com.homestay.repository.HomestayRepository;
import com.homestay.repository.ReviewRepository;
import com.homestay.repository.RoomRepository;
import com.homestay.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCenterService {

    private final UserRepository userRepository;
    private final HomestayRepository homestayRepository;
    private final RoomRepository roomRepository;
    private final BookingOrderRepository bookingOrderRepository;
    private final BookingOrderRoomRepository bookingOrderRoomRepository;
    private final FavoriteRepository favoriteRepository;
    private final ReviewRepository reviewRepository;
    private final PortalService portalService;

    public UserCenterService(
        UserRepository userRepository,
        HomestayRepository homestayRepository,
        RoomRepository roomRepository,
        BookingOrderRepository bookingOrderRepository,
        BookingOrderRoomRepository bookingOrderRoomRepository,
        FavoriteRepository favoriteRepository,
        ReviewRepository reviewRepository,
        PortalService portalService
    ) {
        this.userRepository = userRepository;
        this.homestayRepository = homestayRepository;
        this.roomRepository = roomRepository;
        this.bookingOrderRepository = bookingOrderRepository;
        this.bookingOrderRoomRepository = bookingOrderRoomRepository;
        this.favoriteRepository = favoriteRepository;
        this.reviewRepository = reviewRepository;
        this.portalService = portalService;
    }

    public Map<String, Object> profile(User user) {
        return Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "nickname", user.getNickname(),
            "phone", user.getPhone() == null ? "" : user.getPhone(),
            "avatar", user.getAvatar() == null ? "" : user.getAvatar(),
            "role", user.getRole().name()
        );
    }

    @Transactional
    public Map<String, Object> updateProfile(User user, ProfileUpdateRequest request) {
        user.setNickname(request.nickname());
        user.setAvatar(request.avatar());
        user.setPhone(request.phone());
        userRepository.save(user);
        return profile(user);
    }

    @Transactional
    public Map<String, Object> toggleFavorite(User user, Long homestayId) {
        Homestay homestay = homestayRepository.findById(homestayId)
            .orElseThrow(() -> new BusinessException("房源不存在"));
        Favorite existing = favoriteRepository.findByUserAndHomestay(user, homestay).orElse(null);
        boolean favorite;
        if (existing == null) {
            Favorite item = new Favorite();
            item.setUser(user);
            item.setHomestay(homestay);
            favoriteRepository.save(item);
            favorite = true;
        } else {
            favoriteRepository.delete(existing);
            favorite = false;
        }
        homestay.setFavoriteCount((int) favoriteRepository.countByHomestay(homestay));
        homestayRepository.save(homestay);
        return Map.of("favorite", favorite, "favoriteCount", homestay.getFavoriteCount());
    }

    @Transactional
    public Map<String, Object> createOrder(User user, BookingCreateRequest request) {
        Homestay homestay = homestayRepository.findById(request.homestayId())
            .orElseThrow(() -> new BusinessException("房源不存在"));
        if (!request.checkOutDate().isAfter(request.checkInDate())) {
            throw new BusinessException("退房日期必须晚于入住日期");
        }
        List<Map<String, Object>> availableRooms = portalService.availableRooms(homestay.getId(), request.checkInDate(), request.checkOutDate());
        List<Long> availableRoomIds = availableRooms.stream().map(item -> Long.valueOf(String.valueOf(item.get("id")))).toList();
        if (!availableRoomIds.containsAll(request.roomIds())) {
            throw new BusinessException("部分房间已被预订，请重新选择");
        }
        List<Room> rooms = roomRepository.findAllById(request.roomIds());
        if (rooms.size() != request.roomIds().size()) {
            throw new BusinessException("房间数据不存在");
        }
        long nights = ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());
        BigDecimal totalAmount = rooms.stream()
            .map(Room::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .multiply(BigDecimal.valueOf(nights));

        BookingOrder order = new BookingOrder();
        order.setOrderNo("HS" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        order.setUser(user);
        order.setHomestay(homestay);
        order.setCheckInDate(request.checkInDate());
        order.setCheckOutDate(request.checkOutDate());
        order.setNights((int) nights);
        order.setRoomCount(rooms.size());
        order.setTotalAmount(totalAmount);
        order.setContactName(request.contactName());
        order.setContactPhone(request.contactPhone());
        order.setRemark(request.remark());
        bookingOrderRepository.save(order);

        for (Room room : rooms) {
            BookingOrderRoom item = new BookingOrderRoom();
            item.setOrder(order);
            item.setRoom(room);
            item.setRoomNo(room.getRoomNo());
            bookingOrderRoomRepository.save(item);
        }

        homestay.setBookingCount(homestay.getBookingCount() + 1);
        homestayRepository.save(homestay);
        return orderSummary(order);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> orders(User user) {
        return bookingOrderRepository.findByUserOrderByCreatedAtDesc(user).stream()
            .map(this::orderSummary)
            .toList();
    }

    @Transactional
    public Map<String, Object> payOrder(User user, Long orderId) {
        BookingOrder order = ownOrder(user, orderId);
        if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException("当前订单状态不允许支付");
        }
        order.setOrderStatus(OrderStatus.PAID);
        order.setPaymentStatus(PaymentStatus.PAID);
        bookingOrderRepository.save(order);
        return orderSummary(order);
    }

    @Transactional
    public Map<String, Object> cancelOrder(User user, Long orderId) {
        BookingOrder order = ownOrder(user, orderId);
        if (!(order.getOrderStatus() == OrderStatus.PENDING_PAYMENT || order.getOrderStatus() == OrderStatus.PAID)) {
            throw new BusinessException("当前订单不可取消");
        }
        if (order.getOrderStatus() == OrderStatus.PAID) {
            LocalDate now = LocalDate.now();
            if (!order.getCheckInDate().isAfter(now)) {
                throw new BusinessException("入住当天及之后不可取消已支付订单，如需退款请联系房东");
            }
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        bookingOrderRepository.save(order);
        return orderSummary(order);
    }

    @Transactional
    public Map<String, Object> completeOrder(User user, Long orderId) {
        BookingOrder order = ownOrder(user, orderId);
        if (!(order.getOrderStatus() == OrderStatus.PAID || order.getOrderStatus() == OrderStatus.CONFIRMED)) {
            throw new BusinessException("当前订单不可完成");
        }
        order.setOrderStatus(OrderStatus.COMPLETED);
        bookingOrderRepository.save(order);
        return orderSummary(order);
    }

    @Transactional
    public Map<String, Object> createReview(User user, ReviewCreateRequest request) {
        BookingOrder order = ownOrder(user, request.orderId());
        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException("订单完成后才能评价");
        }
        reviewRepository.findByOrder(order).ifPresent(review -> {
            throw new BusinessException("该订单已评价");
        });
        Review review = new Review();
        review.setOrder(order);
        review.setUser(user);
        review.setHomestay(order.getHomestay());
        review.setScore(request.score());
        review.setContent(request.content());
        review.setImageUrls(request.imageUrls() == null ? "" : String.join(",", request.imageUrls()));
        review.setStatus(ReviewStatus.APPROVED);
        reviewRepository.save(review);
        portalService.recalculateHomestayRating(order.getHomestay());
        return Map.of("id", review.getId(), "score", review.getScore());
    }

    private BookingOrder ownOrder(User user, Long orderId) {
        BookingOrder order = bookingOrderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException("订单不存在"));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new BusinessException("无权操作该订单");
        }
        return order;
    }

    private Map<String, Object> orderSummary(BookingOrder order) {
        List<BookingOrderRoom> rooms = bookingOrderRoomRepository.findByOrder(order);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", order.getId());
        data.put("orderNo", order.getOrderNo());
        data.put("homestayId", order.getHomestay().getId());
        data.put("homestayName", order.getHomestay().getName());
        data.put("coverImage", order.getHomestay().getCoverImage());
        data.put("checkInDate", order.getCheckInDate());
        data.put("checkOutDate", order.getCheckOutDate());
        data.put("nights", order.getNights());
        data.put("roomCount", order.getRoomCount());
        data.put("roomNos", rooms.stream().map(BookingOrderRoom::getRoomNo).toList());
        data.put("totalAmount", order.getTotalAmount());
        data.put("orderStatus", order.getOrderStatus().name());
        data.put("paymentStatus", order.getPaymentStatus().name());
        data.put("contactName", order.getContactName() == null ? "" : order.getContactName());
        data.put("contactPhone", order.getContactPhone() == null ? "" : order.getContactPhone());
        data.put("createdAt", order.getCreatedAt());
        data.put("reviewed", reviewRepository.findByOrder(order).isPresent());
        return data;
    }
}
