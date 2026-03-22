package com.homestay.service;

import com.homestay.common.BusinessException;
import com.homestay.dto.OrderDtos.BookingCreateRequest;
import com.homestay.dto.OrderDtos.ReviewCreateRequest;
import com.homestay.dto.UserDtos.PasswordChangeRequest;
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
import com.homestay.repository.HomestayImageRepository;
import com.homestay.repository.HostApplicationRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;
    private final HomestayImageRepository homestayImageRepository;
    private final HostApplicationRepository hostApplicationRepository;

    public UserCenterService(
        UserRepository userRepository,
        HomestayRepository homestayRepository,
        RoomRepository roomRepository,
        BookingOrderRepository bookingOrderRepository,
        BookingOrderRoomRepository bookingOrderRoomRepository,
        FavoriteRepository favoriteRepository,
        ReviewRepository reviewRepository,
        PortalService portalService,
        PasswordEncoder passwordEncoder,
        HomestayImageRepository homestayImageRepository,
        HostApplicationRepository hostApplicationRepository
    ) {
        this.userRepository = userRepository;
        this.homestayRepository = homestayRepository;
        this.roomRepository = roomRepository;
        this.bookingOrderRepository = bookingOrderRepository;
        this.bookingOrderRoomRepository = bookingOrderRoomRepository;
        this.favoriteRepository = favoriteRepository;
        this.reviewRepository = reviewRepository;
        this.portalService = portalService;
        this.passwordEncoder = passwordEncoder;
        this.homestayImageRepository = homestayImageRepository;
        this.hostApplicationRepository = hostApplicationRepository;
    }

    public Map<String, Object> profile(User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("phone", user.getPhone() == null ? "" : user.getPhone());
        data.put("avatar", user.getAvatar() == null ? "" : user.getAvatar());
        data.put("role", user.getRole().name());
        data.put("blacklisted", Boolean.TRUE.equals(user.getBlacklisted()));
        return data;
    }

    @Transactional
    public Map<String, Object> updateProfile(User user, ProfileUpdateRequest request) {
        user.setNickname(request.getNickname());
        user.setAvatar(request.getAvatar());
        user.setPhone(request.getPhone());
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
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("favorite", favorite);
        result.put("favoriteCount", homestay.getFavoriteCount());
        return result;
    }

    @Transactional
    public Map<String, Object> createOrder(User user, BookingCreateRequest request) {
        ensureCanBook(user);
        Homestay homestay = homestayRepository.findById(request.getHomestayId())
            .orElseThrow(() -> new BusinessException("房源不存在"));
        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new BusinessException("退房日期必须晚于入住日期");
        }
        List<Map<String, Object>> availableRooms = portalService.availableRooms(homestay.getId(), request.getCheckInDate(), request.getCheckOutDate());
        List<Long> availableRoomIds = availableRooms.stream().map(item -> Long.valueOf(String.valueOf(item.get("id")))).collect(java.util.stream.Collectors.toList());
        if (!availableRoomIds.containsAll(request.getRoomIds())) {
            throw new BusinessException("部分房间已被预订，请重新选择");
        }
        List<Room> rooms = roomRepository.findAllById(request.getRoomIds());
        if (rooms.size() != request.getRoomIds().size()) {
            throw new BusinessException("房间数据不存在");
        }
        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal totalAmount = rooms.stream()
            .map(Room::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .multiply(BigDecimal.valueOf(nights));

        BookingOrder order = new BookingOrder();
        order.setOrderNo("HS" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        order.setUser(user);
        order.setHomestay(homestay);
        order.setCheckInDate(request.getCheckInDate());
        order.setCheckOutDate(request.getCheckOutDate());
        order.setNights((int) nights);
        order.setRoomCount(rooms.size());
        order.setTotalAmount(totalAmount);
        order.setContactName(request.getContactName());
        order.setContactPhone(request.getContactPhone());
        order.setRemark(request.getRemark());
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
            .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public Map<String, Object> payOrder(User user, Long orderId) {
        ensureCanBook(user);
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
        if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException("已支付订单请发起退款");
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        bookingOrderRepository.save(order);
        return orderSummary(order);
    }

    @Transactional
    public Map<String, Object> refundOrder(User user, Long orderId) {
        BookingOrder order = ownOrder(user, orderId);
        if (order.getOrderStatus() == OrderStatus.REFUND_REQUESTED) {
            throw new BusinessException("退款申请已提交");
        }
        if (order.getOrderStatus() == OrderStatus.REFUNDED) {
            throw new BusinessException("订单已退款");
        }
        if (!(order.getOrderStatus() == OrderStatus.PAID || order.getOrderStatus() == OrderStatus.CONFIRMED)) {
            throw new BusinessException("当前订单不可退款");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = order.getCheckInDate().atTime(LocalTime.NOON);
        if (!now.isBefore(cutoff)) {
            throw new BusinessException("最迟可在入住当天中午 12:00 前申请退款");
        }
        order.setOrderStatus(OrderStatus.REFUND_REQUESTED);
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
    public Map<String, Object> deleteOrder(User user, Long orderId) {
        BookingOrder order = ownOrder(user, orderId);
        if (!(order.getOrderStatus() == OrderStatus.COMPLETED
            || order.getOrderStatus() == OrderStatus.CANCELLED
            || order.getOrderStatus() == OrderStatus.REFUNDED)) {
            throw new BusinessException("当前订单不可删除");
        }

        reviewRepository.findByOrder(order).ifPresent(review -> {
            reviewRepository.delete(review);
            portalService.recalculateHomestayRating(order.getHomestay());
        });
        bookingOrderRoomRepository.deleteAll(bookingOrderRoomRepository.findByOrder(order));
        bookingOrderRepository.delete(order);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", orderId);
        result.put("deleted", true);
        return result;
    }

    @Transactional
    public Map<String, Object> createReview(User user, ReviewCreateRequest request) {
        BookingOrder order = ownOrder(user, request.getOrderId());
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
        review.setScore(request.getScore());
        review.setContent(request.getContent());
        review.setImageUrls(request.getImageUrls() == null ? "" : String.join(",", request.getImageUrls()));
        review.setStatus(ReviewStatus.APPROVED);
        reviewRepository.save(review);
        portalService.recalculateHomestayRating(order.getHomestay());
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", review.getId());
        result.put("score", review.getScore());
        return result;
    }

    @Transactional
    public void deleteAccount(User user) {
        // 1. 检查是否有未完成订单
        List<BookingOrder> activeOrders = bookingOrderRepository.findByUserOrderByCreatedAtDesc(user).stream()
            .filter(order -> order.getOrderStatus() != OrderStatus.COMPLETED 
                && order.getOrderStatus() != OrderStatus.CANCELLED 
                && order.getOrderStatus() != OrderStatus.REFUNDED)
            .collect(java.util.stream.Collectors.toList());
        if (!activeOrders.isEmpty()) {
            throw new BusinessException("您有未完成的订单，请在订单完成后再注销账号");
        }

        // 2. 如果是房东，检查其房源下是否有未完成订单
        if (user.getRole() == com.homestay.enums.RoleType.HOST) {
            List<Homestay> homestays = homestayRepository.findByHost(user);
            for (Homestay h : homestays) {
                List<BookingOrder> hostOrders = bookingOrderRepository.findByHomestayOrderByCreatedAtDesc(h).stream()
                    .filter(order -> order.getOrderStatus() != OrderStatus.COMPLETED 
                        && order.getOrderStatus() != OrderStatus.CANCELLED 
                        && order.getOrderStatus() != OrderStatus.REFUNDED)
                    .collect(java.util.stream.Collectors.toList());
                if (!hostOrders.isEmpty()) {
                    throw new BusinessException("您的房源 [" + h.getName() + "] 存在未完成的订单，无法注销账号");
                }
            }
        }

        // 3. 删除用户本人的订单记录
        List<BookingOrder> userOrders = bookingOrderRepository.findByUserOrderByCreatedAtDesc(user);
        for (BookingOrder order : userOrders) {
            bookingOrderRoomRepository.deleteAll(bookingOrderRoomRepository.findByOrder(order));
            reviewRepository.findByOrder(order).ifPresent(reviewRepository::delete);
            bookingOrderRepository.delete(order);
        }

        // 4. 如果是房东，级联删除其名下的房源和关联记录
        if (user.getRole() == com.homestay.enums.RoleType.HOST) {
            List<Homestay> homestays = homestayRepository.findByHost(user);
            for (Homestay h : homestays) {
                List<BookingOrder> hOrders = bookingOrderRepository.findByHomestayOrderByCreatedAtDesc(h);
                for (BookingOrder order : hOrders) {
                    bookingOrderRoomRepository.deleteAll(bookingOrderRoomRepository.findByOrder(order));
                    reviewRepository.findByOrder(order).ifPresent(reviewRepository::delete);
                    bookingOrderRepository.delete(order);
                }
                homestayImageRepository.deleteAll(homestayImageRepository.findByHomestayOrderBySortOrderAsc(h));
                favoriteRepository.deleteAll(favoriteRepository.findByHomestay(h));
                roomRepository.deleteAll(roomRepository.findByHomestayOrderByRoomNoAsc(h));
                reviewRepository.deleteAll(reviewRepository.findByHomestayAndStatusOrderByCreatedAtDesc(h, ReviewStatus.APPROVED));
                reviewRepository.deleteAll(reviewRepository.findByHomestayAndStatusOrderByCreatedAtDesc(h, ReviewStatus.HIDDEN));
                homestayRepository.delete(h);
            }
        }

        // 5. 删除其他关联数据
        favoriteRepository.deleteByUser(user);
        reviewRepository.deleteAll(reviewRepository.findByUserOrderByCreatedAtDesc(user));
        hostApplicationRepository.deleteByUsername(user.getUsername());

        // 6. 删除用户实体
        userRepository.delete(user);
    }

    @Transactional
    public void changePassword(User user, PasswordChangeRequest request) {
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private BookingOrder ownOrder(User user, Long orderId) {
        BookingOrder order = bookingOrderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException("订单不存在"));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new BusinessException("无权操作该订单");
        }
        return order;
    }

    private void ensureCanBook(User user) {
        if (Boolean.TRUE.equals(user.getBlacklisted())) {
            throw new BusinessException("账号已被拉入黑名单，无法继续订房");
        }
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
        data.put("roomNos", rooms.stream().map(BookingOrderRoom::getRoomNo).collect(java.util.stream.Collectors.toList()));
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
