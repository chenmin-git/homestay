package com.homestay.service;

import com.homestay.common.BusinessException;
import com.homestay.dto.AdminDtos.HomestaySaveRequest;
import com.homestay.entity.Banner;
import com.homestay.entity.BookingOrder;
import com.homestay.entity.Homestay;
import com.homestay.entity.HomestayImage;
import com.homestay.entity.Notice;
import com.homestay.entity.Review;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.enums.HomestayStatus;
import com.homestay.enums.OrderStatus;
import com.homestay.enums.PaymentStatus;
import com.homestay.enums.ReviewStatus;
import com.homestay.repository.BannerRepository;
import com.homestay.repository.BookingOrderRepository;
import com.homestay.repository.BookingOrderRoomRepository;
import com.homestay.repository.HomestayImageRepository;
import com.homestay.repository.HomestayRepository;
import com.homestay.repository.NoticeRepository;
import com.homestay.repository.ReviewRepository;
import com.homestay.repository.RoomRepository;
import com.homestay.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final HomestayRepository homestayRepository;
    private final HomestayImageRepository homestayImageRepository;
    private final RoomRepository roomRepository;
    private final BookingOrderRepository bookingOrderRepository;
    private final BookingOrderRoomRepository bookingOrderRoomRepository;
    private final ReviewRepository reviewRepository;
    private final BannerRepository bannerRepository;
    private final NoticeRepository noticeRepository;

    public Map<String, Object> dashboard() {
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1).minusSeconds(1);
        long todayOrders = bookingOrderRepository.countByCreatedAtBetween(todayStart, todayEnd);
        BigDecimal sales = bookingOrderRepository.findAll().stream()
            .filter(item -> item.getPaymentStatus() == PaymentStatus.PAID || item.getPaymentStatus() == PaymentStatus.REFUNDED)
            .map(BookingOrder::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long newUsers = userRepository.findAll().stream().filter(item -> item.getCreatedAt().isAfter(todayStart)).count();
        List<Map<String, Object>> orderTrend = java.util.stream.IntStream.rangeClosed(0, 6)
            .mapToObj(offset -> {
                LocalDateTime start = todayStart.minusDays(6L - offset);
                LocalDateTime end = start.plusDays(1).minusSeconds(1);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("date", start.toLocalDate());
                item.put("count", bookingOrderRepository.countByCreatedAtBetween(start, end));
                return item;
            }).toList();
        Map<String, Long> typePie = homestayRepository.findAll().stream().collect(
            java.util.stream.Collectors.groupingBy(Homestay::getHouseType, LinkedHashMap::new, java.util.stream.Collectors.counting())
        );
        long pendingOrders = bookingOrderRepository.findAll().stream().filter(item -> item.getOrderStatus() == OrderStatus.PAID).count();
        long newComments = reviewRepository.findAll().stream().filter(item -> item.getStatus() == ReviewStatus.APPROVED).count();
        return Map.of(
            "todayOrders", todayOrders,
            "totalSales", sales,
            "newUsers", newUsers,
            "orderTrend", orderTrend,
            "typePie", typePie.entrySet().stream().map(item -> Map.of("name", item.getKey(), "value", item.getValue())).toList(),
            "todos", Map.of("pendingOrders", pendingOrders, "newComments", newComments)
        );
    }

    public List<Map<String, Object>> homestays(User operator) {
        List<Homestay> homestays = operator.getRole().name().equals("HOST")
            ? homestayRepository.findByHost(operator)
            : homestayRepository.findAll();
        return homestays.stream().map(item -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", item.getId());
            data.put("name", item.getName());
            data.put("city", item.getCity());
            data.put("houseType", item.getHouseType());
            data.put("basePrice", item.getBasePrice());
            data.put("totalRooms", item.getTotalRooms());
            data.put("status", item.getStatus().name());
            data.put("hostName", item.getHost().getNickname());
            data.put("bookingCount", item.getBookingCount());
            return data;
        }).toList();
    }

    public List<Map<String, Object>> orders() {
        return bookingOrderRepository.findAll().stream().map(item -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", item.getId());
            data.put("orderNo", item.getOrderNo());
            data.put("username", item.getUser().getNickname());
            data.put("homestayName", item.getHomestay().getName());
            data.put("checkInDate", item.getCheckInDate());
            data.put("checkOutDate", item.getCheckOutDate());
            data.put("totalAmount", item.getTotalAmount());
            data.put("orderStatus", item.getOrderStatus().name());
            data.put("paymentStatus", item.getPaymentStatus().name());
            return data;
        }).toList();
    }

    public List<Map<String, Object>> users() {
        return userRepository.findAll().stream().map(item -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", item.getId());
            data.put("username", item.getUsername());
            data.put("nickname", item.getNickname());
            data.put("phone", item.getPhone() == null ? "" : item.getPhone());
            data.put("role", item.getRole().name());
            data.put("enabled", item.getEnabled());
            data.put("blacklisted", item.getBlacklisted());
            return data;
        }).toList();
    }

    public List<Map<String, Object>> reviews() {
        return reviewRepository.findAllByOrderByCreatedAtDesc().stream().map(item -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", item.getId());
            data.put("nickname", item.getUser().getNickname());
            data.put("homestayName", item.getHomestay().getName());
            data.put("score", item.getScore());
            data.put("content", item.getContent() == null ? "" : item.getContent());
            data.put("replyContent", item.getReplyContent() == null ? "" : item.getReplyContent());
            data.put("status", item.getStatus().name());
            data.put("createdAt", item.getCreatedAt());
            return data;
        }).toList();
    }

    public Map<String, Object> settings() {
        return Map.of(
            "banners", bannerRepository.findAll(),
            "notices", noticeRepository.findTop5ByPublishedTrueOrderByCreatedAtDesc()
        );
    }

    @Transactional
    public Map<String, Object> createHomestay(User operator, HomestaySaveRequest request) {
        Homestay homestay = new Homestay();
        homestay.setHost(operator);
        homestay.setName(request.name());
        homestay.setCity(request.city());
        homestay.setDistrict(request.district());
        homestay.setAddress(request.address());
        homestay.setBasePrice(request.basePrice());
        homestay.setHouseType(request.houseType());
        homestay.setTags(request.tags());
        homestay.setFacilities(request.facilities());
        homestay.setLatitude(request.latitude());
        homestay.setLongitude(request.longitude());
        homestay.setCoverImage(request.coverImage());
        homestay.setSummary(request.summary());
        homestay.setDescription(request.description());
        homestay.setRecommended(false);
        homestay.setLatestListed(true);
        homestay.setStatus(HomestayStatus.ONLINE);
        homestay.setTotalRooms(request.rooms().size());
        homestay = homestayRepository.save(homestay);

        int sortOrder = 1;
        for (String imageUrl : request.images()) {
            createImage(homestay, imageUrl, sortOrder++);
        }
        for (var roomForm : request.rooms()) {
            Room room = new Room();
            room.setHomestay(homestay);
            room.setRoomNo(roomForm.roomNo());
            room.setRoomType(roomForm.roomType());
            room.setFloorNo(roomForm.floorNo());
            room.setPrice(roomForm.price());
            room.setBedCount(roomForm.bedCount());
            room.setCapacity(roomForm.capacity());
            roomRepository.save(room);
        }
        return Map.of("id", homestay.getId(), "name", homestay.getName(), "totalRooms", homestay.getTotalRooms());
    }

    @Transactional
    public Map<String, Object> confirmOrder(Long orderId) {
        BookingOrder order = bookingOrderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException("订单不存在"));
        order.setOrderStatus(OrderStatus.CONFIRMED);
        bookingOrderRepository.save(order);
        return Map.of("id", order.getId(), "orderStatus", order.getOrderStatus().name());
    }

    @Transactional
    public Map<String, Object> refundOrder(Long orderId) {
        BookingOrder order = bookingOrderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException("订单不存在"));
        order.setOrderStatus(OrderStatus.REFUNDED);
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        bookingOrderRepository.save(order);
        return Map.of("id", order.getId(), "paymentStatus", order.getPaymentStatus().name());
    }

    @Transactional
    public Map<String, Object> toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        user.setEnabled(!user.getEnabled());
        userRepository.save(user);
        return Map.of("id", user.getId(), "enabled", user.getEnabled());
    }

    @Transactional
    public Map<String, Object> toggleBlacklist(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        user.setBlacklisted(!user.getBlacklisted());
        userRepository.save(user);
        return Map.of("id", user.getId(), "blacklisted", user.getBlacklisted());
    }

    @Transactional
    public Map<String, Object> replyReview(Long reviewId, String content) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(() -> new BusinessException("评论不存在"));
        review.setReplyContent(content);
        reviewRepository.save(review);
        return Map.of("id", review.getId(), "replyContent", review.getReplyContent());
    }

    @Transactional
    public void hideReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(() -> new BusinessException("评论不存在"));
        review.setStatus(ReviewStatus.HIDDEN);
        reviewRepository.save(review);
    }

    @Transactional
    public void seedIfEmpty(User admin, User host) {
        if (homestayRepository.count() > 0) {
            return;
        }

        Banner banner1 = new Banner();
        banner1.setTitle("山海轻居");
        banner1.setImageUrl("https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80");
        banner1.setSortOrder(1);
        bannerRepository.save(banner1);

        Banner banner2 = new Banner();
        banner2.setTitle("江南院落");
        banner2.setImageUrl("https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80");
        banner2.setSortOrder(2);
        bannerRepository.save(banner2);

        Notice notice = new Notice();
        notice.setTitle("春季活动");
        notice.setContent("连续入住两晚可享接站服务，支持按房号选择房间。");
        noticeRepository.save(notice);

        Homestay villa = createHomestay(host, "云栖海景别院", "三亚", "海棠湾", "海棠湾林旺大道 18 号",
            BigDecimal.valueOf(688), 4, "整栋别墅", "海景,泳池,家庭聚会", "泳池,厨房,停车位,洗衣机,投影",
            18.3197, 109.7514,
            "https://images.unsplash.com/photo-1499793983690-e29da59ef1c2?auto=format&fit=crop&w=1200&q=80",
            "适合家庭出游的临海别院，支持按房号预订。",
            "房东可以管理多间客房，用户下单时可直接选择具体房号，避免一房源只能住一组客人的老旧模式。");

        createRoom(villa, "A101", "海景大床房", 1, 688, 1, 2);
        createRoom(villa, "A102", "海景双床房", 1, 728, 2, 3);
        createRoom(villa, "B201", "泳池亲子房", 2, 888, 2, 4);
        createRoom(villa, "B202", "露台套房", 2, 998, 1, 2);

        createImage(villa, "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80", 1);
        createImage(villa, "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80", 2);

        Homestay courtyard = createHomestay(host, "山居慢宿", "丽江", "束河古镇", "束河古镇清泉路 9 号",
            BigDecimal.valueOf(368), 3, "Loft", "古镇,庭院,安静", "地暖,茶室,接送机,投影",
            26.9252, 100.2050,
            "https://images.unsplash.com/photo-1502005229762-cf1b2da7c5d6?auto=format&fit=crop&w=1200&q=80",
            "古镇边上的安静院落，适合情侣与小团体。",
            "支持房东发布多间房源并单独维护房态日历。");
        createRoom(courtyard, "C301", "庭院大床房", 3, 368, 1, 2);
        createRoom(courtyard, "C302", "观景双床房", 3, 428, 2, 2);
        createRoom(courtyard, "C303", "Loft 家庭房", 3, 518, 2, 4);

        BookingOrder order = new BookingOrder();
        order.setOrderNo("HSDEMO202603180001");
        order.setUser(admin);
        order.setHomestay(villa);
        order.setCheckInDate(java.time.LocalDate.now().plusDays(2));
        order.setCheckOutDate(java.time.LocalDate.now().plusDays(4));
        order.setNights(2);
        order.setRoomCount(1);
        order.setTotalAmount(BigDecimal.valueOf(1376));
        order.setOrderStatus(OrderStatus.PAID);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setContactName("系统演示用户");
        order.setContactPhone("13800000000");
        bookingOrderRepository.save(order);

        Room demoRoom = roomRepository.findByHomestayAndEnabledTrueOrderByRoomNoAsc(villa).get(0);
        com.homestay.entity.BookingOrderRoom item = new com.homestay.entity.BookingOrderRoom();
        item.setOrder(order);
        item.setRoom(demoRoom);
        item.setRoomNo(demoRoom.getRoomNo());
        bookingOrderRoomRepository.save(item);

        Review review = new Review();
        review.setOrder(order);
        review.setUser(admin);
        review.setHomestay(villa);
        review.setScore(5);
        review.setContent("位置好找，支持自己选房号这一点很实用。");
        review.setStatus(ReviewStatus.APPROVED);
        reviewRepository.save(review);
        villa.setRating(5D);
        villa.setBookingCount(1);
        homestayRepository.save(villa);
    }

    private Homestay createHomestay(
        User host,
        String name,
        String city,
        String district,
        String address,
        BigDecimal price,
        int rooms,
        String houseType,
        String tags,
        String facilities,
        double lat,
        double lng,
        String cover,
        String summary,
        String description
    ) {
        Homestay homestay = new Homestay();
        homestay.setHost(host);
        homestay.setName(name);
        homestay.setCity(city);
        homestay.setDistrict(district);
        homestay.setAddress(address);
        homestay.setBasePrice(price);
        homestay.setTotalRooms(rooms);
        homestay.setHouseType(houseType);
        homestay.setTags(tags);
        homestay.setFacilities(facilities);
        homestay.setLatitude(lat);
        homestay.setLongitude(lng);
        homestay.setRecommended(true);
        homestay.setLatestListed(true);
        homestay.setCoverImage(cover);
        homestay.setSummary(summary);
        homestay.setDescription(description);
        homestay.setStatus(HomestayStatus.ONLINE);
        return homestayRepository.save(homestay);
    }

    private void createRoom(Homestay homestay, String roomNo, String roomType, int floor, int price, int beds, int capacity) {
        Room room = new Room();
        room.setHomestay(homestay);
        room.setRoomNo(roomNo);
        room.setRoomType(roomType);
        room.setFloorNo(floor);
        room.setPrice(BigDecimal.valueOf(price));
        room.setBedCount(beds);
        room.setCapacity(capacity);
        roomRepository.save(room);
    }

    private void createImage(Homestay homestay, String imageUrl, int sortOrder) {
        HomestayImage image = new HomestayImage();
        image.setHomestay(homestay);
        image.setImageUrl(imageUrl);
        image.setSortOrder(sortOrder);
        homestayImageRepository.save(image);
    }
}
