package com.homestay.service;

import com.homestay.common.BusinessException;
import com.homestay.dto.AdminDtos.BannerSaveRequest;
import com.homestay.dto.AdminDtos.HomestaySaveRequest;
import com.homestay.dto.AdminDtos.NoticeSaveRequest;
import com.homestay.dto.AdminDtos.PasswordChangeRequest;
import com.homestay.dto.AdminDtos.RoomForm;
import com.homestay.entity.Banner;
import com.homestay.entity.BookingOrder;
import com.homestay.entity.BookingOrderRoom;
import com.homestay.entity.Favorite;
import com.homestay.entity.HostApplication;
import com.homestay.entity.Homestay;
import com.homestay.entity.HomestayImage;
import com.homestay.entity.Notice;
import com.homestay.entity.Review;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.enums.HostApplyStatus;
import com.homestay.enums.HomestayStatus;
import com.homestay.enums.OrderStatus;
import com.homestay.enums.PaymentStatus;
import com.homestay.enums.ReviewStatus;
import com.homestay.enums.RoleType;
import com.homestay.repository.BannerRepository;
import com.homestay.repository.BookingOrderRepository;
import com.homestay.repository.BookingOrderRoomRepository;
import com.homestay.repository.FavoriteRepository;
import com.homestay.repository.HostApplicationRepository;
import com.homestay.repository.HomestayImageRepository;
import com.homestay.repository.HomestayRepository;
import com.homestay.repository.NoticeRepository;
import com.homestay.repository.ReviewRepository;
import com.homestay.repository.RoomRepository;
import com.homestay.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
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
    private final FavoriteRepository favoriteRepository;
    private final HostApplicationRepository hostApplicationRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public AdminService(
        UserRepository userRepository,
        HomestayRepository homestayRepository,
        HomestayImageRepository homestayImageRepository,
        RoomRepository roomRepository,
        BookingOrderRepository bookingOrderRepository,
        BookingOrderRoomRepository bookingOrderRoomRepository,
        ReviewRepository reviewRepository,
        BannerRepository bannerRepository,
        NoticeRepository noticeRepository,
        FavoriteRepository favoriteRepository,
        HostApplicationRepository hostApplicationRepository,
        org.springframework.security.crypto.password.PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.homestayRepository = homestayRepository;
        this.homestayImageRepository = homestayImageRepository;
        this.roomRepository = roomRepository;
        this.bookingOrderRepository = bookingOrderRepository;
        this.bookingOrderRoomRepository = bookingOrderRoomRepository;
        this.reviewRepository = reviewRepository;
        this.bannerRepository = bannerRepository;
        this.noticeRepository = noticeRepository;
        this.favoriteRepository = favoriteRepository;
        this.hostApplicationRepository = hostApplicationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard(User operator) {
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1).minusSeconds(1);
        boolean hostOnly = operator.getRole() == RoleType.HOST;
        List<Homestay> homestays = hostOnly
            ? homestayRepository.findByHost(operator)
            : homestayRepository.findAll();
        List<BookingOrder> orders = filterOrdersForOperator(operator);
        List<Review> reviews = filterReviewsForOperator(operator);

        long todayOrders = orders.stream()
            .filter(item -> !item.getCreatedAt().isBefore(todayStart) && !item.getCreatedAt().isAfter(todayEnd))
            .count();
        BigDecimal sales = orders.stream()
            .filter(item -> item.getPaymentStatus() == PaymentStatus.PAID)
            .map(BookingOrder::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todaySales = orders.stream()
            .filter(item -> item.getPaymentStatus() == PaymentStatus.PAID)
            .filter(item -> !item.getCreatedAt().isBefore(todayStart) && !item.getCreatedAt().isAfter(todayEnd))
            .map(BookingOrder::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long newUsers = hostOnly
            ? orders.stream()
                .map(BookingOrder::getUser)
                .filter(item -> item.getCreatedAt().isAfter(todayStart))
                .map(User::getId)
                .distinct()
                .count()
            : userRepository.findAll().stream().filter(item -> item.getCreatedAt().isAfter(todayStart)).count();
        List<Map<String, Object>> orderTrend = java.util.stream.IntStream.rangeClosed(0, 6)
            .mapToObj(offset -> {
                LocalDateTime start = todayStart.minusDays(6L - offset);
                LocalDateTime end = start.plusDays(1).minusSeconds(1);
                long count = orders.stream()
                    .filter(item -> !item.getCreatedAt().isBefore(start) && !item.getCreatedAt().isAfter(end))
                    .count();
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("date", start.toLocalDate());
                item.put("count", count);
                return item;
            }).toList();
        Map<String, Long> typePie = homestays.stream().collect(
            java.util.stream.Collectors.groupingBy(Homestay::getHouseType, LinkedHashMap::new, java.util.stream.Collectors.counting())
        );
        long pendingOrders = orders.stream().filter(item -> item.getOrderStatus() == OrderStatus.PAID).count();
        long newComments = reviews.stream().filter(item -> item.getStatus() == ReviewStatus.APPROVED).count();
        return Map.of(
            "todayOrders", todayOrders,
            "todaySales", todaySales,
            "totalSales", sales,
            "newUsers", newUsers,
            "orderTrend", orderTrend,
            "typePie", typePie.entrySet().stream().map(item -> Map.of("name", item.getKey(), "value", item.getValue())).toList(),
            "todos", Map.of("pendingOrders", pendingOrders, "newComments", newComments)
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> homestays(User operator) {
        List<Homestay> homestays = operator.getRole() == RoleType.HOST
            ? homestayRepository.findByHost(operator)
            : homestayRepository.findAll();
        return homestays.stream().map(this::homestaySummary).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> homestayDetail(User operator, Long homestayId) {
        Homestay homestay = loadOwnedHomestay(operator, homestayId);
        return homestayEditorData(homestay);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> orders(User operator) {
        return filterOrdersForOperator(operator).stream().map(item -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", item.getId());
            data.put("orderNo", item.getOrderNo());
            data.put("username", item.getUser().getNickname());
            data.put("homestayName", item.getHomestay().getName());
            data.put("createdAt", item.getCreatedAt());
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

    @Transactional(readOnly = true)
    public List<Map<String, Object>> hostApplications(User operator) {
        ensureAdmin(operator);
        return hostApplicationRepository.findAllByOrderByCreatedAtDesc().stream().map(item -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", item.getId());
            data.put("username", item.getUsername());
            data.put("nickname", item.getNickname());
            data.put("phone", item.getPhone());
            data.put("status", item.getStatus().name());
            data.put("createdAt", item.getCreatedAt());
            data.put("reviewedAt", item.getReviewedAt());
            return data;
        }).toList();
    }

    @Transactional
    public Map<String, Object> approveHostApplication(User operator, Long applicationId) {
        ensureAdmin(operator);
        HostApplication application = hostApplicationRepository.findById(applicationId)
            .orElseThrow(() -> new BusinessException("申请不存在"));
        if (application.getStatus() != HostApplyStatus.PENDING) {
            throw new BusinessException("该申请已处理");
        }
        userRepository.findByUsername(application.getUsername()).ifPresent(user -> {
            throw new BusinessException("用户名已存在");
        });
        User host = new User();
        host.setUsername(application.getUsername());
        host.setPassword(application.getPassword());
        host.setNickname(application.getNickname());
        host.setPhone(application.getPhone());
        host.setRole(RoleType.HOST);
        host.setEnabled(true);
        host.setBlacklisted(false);
        userRepository.save(host);

        application.setStatus(HostApplyStatus.APPROVED);
        application.setReviewedAt(LocalDateTime.now());
        hostApplicationRepository.save(application);
        return Map.of("id", application.getId(), "status", application.getStatus().name());
    }

    @Transactional
    public Map<String, Object> rejectHostApplication(User operator, Long applicationId) {
        ensureAdmin(operator);
        HostApplication application = hostApplicationRepository.findById(applicationId)
            .orElseThrow(() -> new BusinessException("申请不存在"));
        if (application.getStatus() != HostApplyStatus.PENDING) {
            throw new BusinessException("该申请已处理");
        }
        application.setStatus(HostApplyStatus.REJECTED);
        application.setReviewedAt(LocalDateTime.now());
        hostApplicationRepository.save(application);
        return Map.of("id", application.getId(), "status", application.getStatus().name());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> reviews(User operator) {
        return filterReviewsForOperator(operator).stream().map(item -> {
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
        String normalizedName = request.name().trim();
        if (homestayRepository.existsByHostAndNameIgnoreCase(operator, normalizedName)) {
            throw new BusinessException("同名房源已存在，请修改名称");
        }
        Homestay homestay = new Homestay();
        homestay.setHost(operator);
        homestay.setRecommended(false);
        homestay.setLatestListed(true);
        homestay.setStatus(operator.getRole() == RoleType.HOST ? HomestayStatus.DRAFT : HomestayStatus.ONLINE);
        applyHomestayBaseInfo(homestay, request);
        homestay = homestayRepository.save(homestay);
        saveHomestayImages(homestay, request.images());
        syncRooms(homestay, request.rooms());
        homestayRepository.save(homestay);
        return homestayEditorData(homestay);
    }

    @Transactional
    public Map<String, Object> updateHomestay(User operator, Long homestayId, HomestaySaveRequest request) {
        Homestay homestay = loadOwnedHomestay(operator, homestayId);
        String normalizedName = request.name().trim();
        if (homestayRepository.existsByHostAndNameIgnoreCaseAndIdNot(homestay.getHost(), normalizedName, homestayId)) {
            throw new BusinessException("同名房源已存在，请修改名称");
        }
        applyHomestayBaseInfo(homestay, request);
        if (operator.getRole() == RoleType.HOST) {
            homestay.setStatus(HomestayStatus.DRAFT);
        }
        homestayRepository.save(homestay);
        saveHomestayImages(homestay, request.images());
        syncRooms(homestay, request.rooms());
        homestayRepository.save(homestay);
        return homestayEditorData(homestay);
    }

    @Transactional
    public Map<String, Object> toggleHomestayStatus(User operator, Long homestayId) {
        Homestay homestay = loadOwnedHomestay(operator, homestayId);
        if (homestay.getStatus() == HomestayStatus.DRAFT) {
            if (operator.getRole() != RoleType.ADMIN) {
                throw new BusinessException("房源待审核，管理员审核后才能上架");
            }
            homestay.setStatus(HomestayStatus.ONLINE);
            homestayRepository.save(homestay);
            return Map.of("id", homestay.getId(), "status", homestay.getStatus().name());
        }
        HomestayStatus nextStatus = homestay.getStatus() == HomestayStatus.ONLINE
            ? HomestayStatus.OFFLINE
            : HomestayStatus.ONLINE;
        homestay.setStatus(nextStatus);
        homestayRepository.save(homestay);
        return Map.of("id", homestay.getId(), "status", homestay.getStatus().name());
    }

    @Transactional
    public Map<String, Object> deleteHomestay(User operator, Long homestayId) {
        Homestay homestay = loadOwnedHomestay(operator, homestayId);
        if (!bookingOrderRepository.findByHomestayOrderByCreatedAtDesc(homestay).isEmpty()) {
            throw new BusinessException("该房源已有订单记录，不能直接删除，请先下架");
        }
        if (!reviewRepository.findByHomestayOrderByCreatedAtDesc(homestay).isEmpty()) {
            throw new BusinessException("该房源已有评论记录，不能直接删除");
        }
        homestayImageRepository.deleteAll(homestayImageRepository.findByHomestayOrderBySortOrderAsc(homestay));
        favoriteRepository.deleteAll(favoriteRepository.findByHomestay(homestay));
        roomRepository.deleteAll(roomRepository.findByHomestayOrderByRoomNoAsc(homestay));
        homestayRepository.delete(homestay);
        return Map.of("id", homestayId, "deleted", true);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> homestayCalendar(User operator, Long homestayId, LocalDate startDate, int days) {
        Homestay homestay = loadOwnedHomestay(operator, homestayId);
        LocalDate calendarStart = startDate == null ? LocalDate.now() : startDate;
        int displayDays = Math.max(3, Math.min(days, 14));
        LocalDate calendarEndExclusive = calendarStart.plusDays(displayDays);

        List<Room> rooms = roomRepository.findByHomestayAndEnabledTrueOrderByRoomNoAsc(homestay);
        List<BookingOrder> bookings = bookingOrderRepository.findByHomestayOrderByCreatedAtDesc(homestay).stream()
            .filter(this::shouldOccupyInventory)
            .filter(order -> order.getCheckInDate().isBefore(calendarEndExclusive) && order.getCheckOutDate().isAfter(calendarStart))
            .toList();
        List<BookingOrderRoom> bookingRooms = bookings.isEmpty() ? List.of() : bookingOrderRoomRepository.findByOrderIn(bookings);

        Map<Long, List<BookingOrder>> roomOrderMap = new LinkedHashMap<>();
        for (BookingOrderRoom binding : bookingRooms) {
            roomOrderMap.computeIfAbsent(binding.getRoom().getId(), key -> new ArrayList<>()).add(binding.getOrder());
        }

        List<LocalDate> dates = new ArrayList<>();
        for (int i = 0; i < displayDays; i++) {
            dates.add(calendarStart.plusDays(i));
        }

        List<Map<String, Object>> roomRows = new ArrayList<>();
        for (Room room : rooms) {
            List<BookingOrder> roomBookings = new ArrayList<>(roomOrderMap.getOrDefault(room.getId(), List.of()));
            roomBookings.sort((left, right) -> left.getCheckInDate().compareTo(right.getCheckInDate()));

            List<Map<String, Object>> slots = new ArrayList<>();
            for (LocalDate day : dates) {
                BookingOrder match = null;
                for (BookingOrder booking : roomBookings) {
                    if (!day.isBefore(booking.getCheckInDate()) && day.isBefore(booking.getCheckOutDate())) {
                        match = booking;
                        break;
                    }
                }
                Map<String, Object> slot = new LinkedHashMap<>();
                slot.put("date", day);
                slot.put("occupied", match != null);
                slot.put("orderNo", match == null ? "" : match.getOrderNo());
                slot.put("guestName", match == null ? "" : match.getUser().getNickname());
                slot.put("status", match == null ? "AVAILABLE" : match.getOrderStatus().name());
                slots.add(slot);
            }

            List<Map<String, Object>> bookingItems = new ArrayList<>();
            for (BookingOrder booking : roomBookings) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("orderId", booking.getId());
                item.put("orderNo", booking.getOrderNo());
                item.put("guestName", booking.getUser().getNickname());
                item.put("checkInDate", booking.getCheckInDate());
                item.put("checkOutDate", booking.getCheckOutDate());
                item.put("status", booking.getOrderStatus().name());
                bookingItems.add(item);
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", room.getId());
            row.put("roomNo", room.getRoomNo());
            row.put("roomType", room.getRoomType());
            row.put("floorNo", room.getFloorNo());
            row.put("price", room.getPrice());
            row.put("bookings", bookingItems);
            row.put("slots", slots);
            roomRows.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("homestayId", homestay.getId());
        result.put("homestayName", homestay.getName());
        result.put("startDate", calendarStart);
        result.put("days", displayDays);
        result.put("dates", dates);
        result.put("rooms", roomRows);
        return result;
    }

    @Transactional
    public Map<String, Object> confirmOrder(User operator, Long orderId) {
        if (operator.getRole() != RoleType.HOST) {
            throw new BusinessException("仅房东可确认入住");
        }
        BookingOrder order = loadOwnedOrder(operator, orderId);
        order.setOrderStatus(OrderStatus.CONFIRMED);
        bookingOrderRepository.save(order);
        return Map.of("id", order.getId(), "orderStatus", order.getOrderStatus().name());
    }

    @Transactional
    public Map<String, Object> refundOrder(User operator, Long orderId) {
        if (operator.getRole() != RoleType.HOST) {
            throw new BusinessException("仅房东可处理退款");
        }
        BookingOrder order = loadOwnedOrder(operator, orderId);
        if (order.getOrderStatus() != OrderStatus.REFUND_REQUESTED) {
            throw new BusinessException("请先由用户发起退款申请");
        }
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
    public Map<String, Object> replyReview(User operator, Long reviewId, String content) {
        Review review = loadOwnedReview(operator, reviewId);
        review.setReplyContent(content);
        reviewRepository.save(review);
        return Map.of("id", review.getId(), "replyContent", review.getReplyContent());
    }

    @Transactional
    public void hideReview(User operator, Long reviewId) {
        Review review = loadOwnedReview(operator, reviewId);
        review.setStatus(ReviewStatus.HIDDEN);
        reviewRepository.save(review);
    }

    @Transactional
    public void updateBanners(List<BannerSaveRequest> requests) {
        bannerRepository.deleteAll();
        for (BannerSaveRequest req : requests) {
            Banner banner = new Banner();
            banner.setTitle(req.title());
            banner.setImageUrl(req.imageUrl());
            banner.setLinkUrl(req.linkUrl());
            banner.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
            banner.setEnabled(req.enabled() == null || req.enabled());
            bannerRepository.save(banner);
        }
    }

    @Transactional
    public void updateNotices(List<NoticeSaveRequest> requests) {
        noticeRepository.deleteAll();
        for (NoticeSaveRequest req : requests) {
            Notice notice = new Notice();
            notice.setTitle(req.title());
            notice.setContent(req.content());
            notice.setPublished(req.published() == null || req.published());
            noticeRepository.save(notice);
        }
    }

    @Transactional
    public void changePassword(User operator, PasswordChangeRequest request) {
        if (!passwordEncoder.matches(request.oldPassword(), operator.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        operator.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(operator);
    }

    @Transactional
    public void seedIfEmpty(User admin, User host) {
        User user = userRepository.findByUsername("user").orElse(null);
        User user2 = userRepository.findByUsername("user2").orElse(user);
        User user3 = userRepository.findByUsername("user3").orElse(user2);
        User host2 = userRepository.findByUsername("host2").orElse(host);

        ensureBanner("山海轻居",
            "/assets/photos/photo-1505693416388.jpg", 1);
        ensureBanner("江南院落",
            "/assets/photos/photo-1522708323590.jpg", 2);
        ensureBanner("城市周末",
            "/assets/photos/photo-1494526585095.jpg", 3);

        ensureNotice("春季活动", "连续入住两晚可享接站服务，支持按房号选择房间。");
        ensureNotice("房态升级", "后台支持按真实房号查看占用情况，适合课程设计演示。");
        ensureNotice("演示说明", "可使用 admin、host、host2、user、user2、user3 等账号体验不同页面。");

        Homestay villa = ensureHomestay(host, "云栖海景别院", "三亚", "海棠湾", "海棠湾林旺大道 18 号",
            BigDecimal.valueOf(688), 4, "整栋别墅", "海景,泳池,家庭聚会", "泳池,厨房,停车位,洗衣机,投影",
            18.3197, 109.7514,
            "/assets/photos/photo-1499793983690.jpg",
            "适合家庭出游的临海别院，支持按房号预订。",
            "房东可以管理多间客房，用户下单时可直接选择具体房号，避免一房源只能住一组客人的老旧模式。");
        ensureRoom(villa, "A101", "海景大床房", 1, 688, 1, 2);
        ensureRoom(villa, "A102", "海景双床房", 1, 728, 2, 3);
        ensureRoom(villa, "B201", "泳池亲子房", 2, 888, 2, 4);
        ensureRoom(villa, "B202", "露台套房", 2, 998, 1, 2);
        ensureImages(villa, List.of(
            "/assets/photos/photo-1505693416388.jpg",
            "/assets/photos/photo-1505693416388.jpg"
        ));

        Homestay courtyard = ensureHomestay(host, "山居慢宿", "丽江", "束河古镇", "束河古镇清泉路 9 号",
            BigDecimal.valueOf(368), 3, "Loft", "古镇,庭院,安静", "地暖,茶室,接送机,投影",
            26.9252, 100.2050,
            "/assets/photos/photo-1502005229762.jpg",
            "古镇边上的安静院落，适合情侣与小团体。",
            "支持房东发布多间房源并单独维护房态日历。");
        ensureRoom(courtyard, "C301", "庭院大床房", 3, 368, 1, 2);
        ensureRoom(courtyard, "C302", "观景双床房", 3, 428, 2, 2);
        ensureRoom(courtyard, "C303", "Loft 家庭房", 3, 518, 2, 4);
        ensureImages(courtyard, List.of(
            "/assets/photos/photo-1502005229762.jpg",
            "/assets/photos/photo-1522708323590.jpg"
        ));

        Homestay cityStay = ensureHomestay(host2, "城景轻奢公寓", "上海", "静安区", "南京西路 188 号",
            BigDecimal.valueOf(588), 3, "城市公寓", "地铁旁,商旅,高层夜景", "电梯,投影,洗衣机,健身房",
            31.2304, 121.4737,
            "/assets/photos/photo-1494526585095.jpg",
            "适合短住与商务出差的高层公寓，支持按房号库存管理。",
            "房东可以维护不同朝向与床型的多个房间，避免城市民宿常见的一套房只能接一单。");
        ensureRoom(cityStay, "S1101", "商务大床房", 11, 588, 1, 2);
        ensureRoom(cityStay, "S1102", "双床商务房", 11, 628, 2, 2);
        ensureRoom(cityStay, "S1201", "城景套房", 12, 788, 1, 3);
        ensureImages(cityStay, List.of(
            "/assets/photos/photo-1494526585095.jpg",
            "/assets/photos/photo-1505693416388.jpg"
        ));

        Homestay lakeVilla = ensureHomestay(host2, "湖畔亲子小院", "杭州", "余杭区", "瓶窑镇山水路 66 号",
            BigDecimal.valueOf(428), 2, "亲子庭院", "亲子,露台,烧烤", "庭院,儿童玩具,厨房,停车位",
            30.3542, 119.9781,
            "/assets/photos/photo-1523217582562.jpg",
            "适合周末亲子出游的小院，房东可独立维护每个房号。",
            "既能满足家庭包院，也能按真实房间库存出售，不会因一笔订单就整套停售。");
        ensureRoom(lakeVilla, "H201", "亲子套房", 2, 428, 2, 3);
        ensureRoom(lakeVilla, "H202", "湖景家庭房", 2, 498, 2, 4);
        ensureImages(lakeVilla, List.of(
            "/assets/photos/photo-1523217582562.jpg",
            "/assets/photos/photo-1505693416388.jpg"
        ));

        BookingOrder paidOrder = ensureOrder(
            "HSDEMO202603180001", admin, villa,
            List.of(findRoomByNo(villa, "A101")),
            LocalDate.now().plusDays(2), LocalDate.now().plusDays(4),
            OrderStatus.PAID, PaymentStatus.PAID, "系统演示用户", "13800000000", "管理员演示订单"
        );
        ensureReview(paidOrder, admin, villa, 5, "位置好找，支持自己选房号这一点很实用。", "", ReviewStatus.APPROVED);

        BookingOrder pendingOrder = ensureOrder(
            "HSDEMO202603180002", user, villa,
            List.of(findRoomByNo(villa, "A102")),
            LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
            OrderStatus.PENDING_PAYMENT, PaymentStatus.UNPAID, "演示游客", "13800138000", "待支付演示"
        );

        BookingOrder confirmedOrder = ensureOrder(
            "HSDEMO202603180003", user2, courtyard,
            List.of(findRoomByNo(courtyard, "C301")),
            LocalDate.now().plusDays(3), LocalDate.now().plusDays(6),
            OrderStatus.CONFIRMED, PaymentStatus.PAID, "差旅白领", "13900000004", "待入住演示"
        );

        BookingOrder completedOrder = ensureOrder(
            "HSDEMO202603180004", user3, cityStay,
            List.of(findRoomByNo(cityStay, "S1201")),
            LocalDate.now().minusDays(10), LocalDate.now().minusDays(7),
            OrderStatus.COMPLETED, PaymentStatus.PAID, "周末旅行家", "13900000005", "已完成演示"
        );
        ensureReview(completedOrder, user3, cityStay, 4, "夜景很好，房间号选择清晰，适合答辩展示。", "感谢入住，欢迎下次再来。", ReviewStatus.APPROVED);

        BookingOrder refundedOrder = ensureOrder(
            "HSDEMO202603180005", user2, lakeVilla,
            List.of(findRoomByNo(lakeVilla, "H201")),
            LocalDate.now().minusDays(5), LocalDate.now().minusDays(3),
            OrderStatus.REFUNDED, PaymentStatus.REFUNDED, "差旅白领", "13900000004", "退款演示"
        );

        BookingOrder cancelledOrder = ensureOrder(
            "HSDEMO202603180006", user, cityStay,
            List.of(findRoomByNo(cityStay, "S1101")),
            LocalDate.now().plusDays(6), LocalDate.now().plusDays(8),
            OrderStatus.CANCELLED, PaymentStatus.UNPAID, "演示游客", "13900000003", "取消演示"
        );

        BookingOrder hiddenReviewOrder = ensureOrder(
            "HSDEMO202603180007", user2, courtyard,
            List.of(findRoomByNo(courtyard, "C302")),
            LocalDate.now().minusDays(12), LocalDate.now().minusDays(9),
            OrderStatus.COMPLETED, PaymentStatus.PAID, "差旅白领", "13900000004", "隐藏评论演示"
        );
        ensureReview(hiddenReviewOrder, user2, courtyard, 2, "环境还行，但我想测试评论管理是否能隐藏。", "", ReviewStatus.HIDDEN);

        ensureFavorite(user, villa);
        ensureFavorite(user, courtyard);
        ensureFavorite(user2, cityStay);
        ensureFavorite(user3, lakeVilla);
        ensureFavorite(admin, villa);

        refreshHomestayStats(villa);
        refreshHomestayStats(courtyard);
        refreshHomestayStats(cityStay);
        refreshHomestayStats(lakeVilla);
    }

    private void ensureBanner(String title, String imageUrl, int sortOrder) {
        Banner banner = bannerRepository.findAll().stream()
            .filter(item -> title.equals(item.getTitle()))
            .findFirst()
            .orElse(null);
        if (banner != null) {
            if (isRemoteUrl(banner.getImageUrl())) {
                banner.setImageUrl(imageUrl);
                banner.setSortOrder(sortOrder);
                bannerRepository.save(banner);
            }
            return;
        }
        Banner created = new Banner();
        created.setTitle(title);
        created.setImageUrl(imageUrl);
        created.setSortOrder(sortOrder);
        bannerRepository.save(created);
    }

    private void ensureNotice(String title, String content) {
        boolean exists = noticeRepository.findAll().stream().anyMatch(item -> title.equals(item.getTitle()));
        if (exists) {
            return;
        }
        Notice notice = new Notice();
        notice.setTitle(title);
        notice.setContent(content);
        noticeRepository.save(notice);
    }

    private Homestay ensureHomestay(
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
        return homestayRepository.findAll().stream()
            .filter(item -> name.equals(item.getName()))
            .findFirst()
            .map(existing -> {
                if (existing.getHost() == null || !existing.getHost().getId().equals(host.getId())) {
                    existing.setHost(host);
                    return homestayRepository.save(existing);
                }
                if (isRemoteUrl(existing.getCoverImage())) {
                    existing.setCoverImage(cover);
                    return homestayRepository.save(existing);
                }
                return existing;
            })
            .orElseGet(() -> createHomestay(host, name, city, district, address, price, rooms, houseType, tags, facilities, lat, lng, cover, summary, description));
    }

    private void ensureRoom(Homestay homestay, String roomNo, String roomType, int floor, int price, int beds, int capacity) {
        boolean exists = roomRepository.findByHomestayOrderByRoomNoAsc(homestay).stream()
            .anyMatch(item -> roomNo.equals(item.getRoomNo()));
        if (!exists) {
            createRoom(homestay, roomNo, roomType, floor, price, beds, capacity);
            homestay.setTotalRooms(roomRepository.findByHomestayOrderByRoomNoAsc(homestay).stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .toList()
                .size());
            homestayRepository.save(homestay);
        }
    }

    private void ensureImages(Homestay homestay, List<String> imageUrls) {
        List<HomestayImage> existing = homestayImageRepository.findByHomestayOrderBySortOrderAsc(homestay);
        if (existing.isEmpty() || existing.stream().anyMatch(item -> isRemoteUrl(item.getImageUrl()))) {
            homestayImageRepository.deleteAll(existing);
            int sortOrder = 1;
            for (String imageUrl : imageUrls) {
                createImage(homestay, imageUrl, sortOrder++);
            }
            return;
        }
        for (int i = 0; i < imageUrls.size(); i++) {
            String imageUrl = imageUrls.get(i);
            boolean exists = existing.stream().anyMatch(item -> imageUrl.equals(item.getImageUrl()));
            if (!exists) {
                createImage(homestay, imageUrl, i + 1);
            }
        }
    }

    private Room findRoomByNo(Homestay homestay, String roomNo) {
        return roomRepository.findByHomestayOrderByRoomNoAsc(homestay).stream()
            .filter(item -> roomNo.equals(item.getRoomNo()))
            .findFirst()
            .orElseThrow(() -> new BusinessException("演示房间不存在: " + roomNo));
    }

    private BookingOrder ensureOrder(
        String orderNo,
        User user,
        Homestay homestay,
        List<Room> rooms,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        OrderStatus orderStatus,
        PaymentStatus paymentStatus,
        String contactName,
        String contactPhone,
        String remark
    ) {
        return bookingOrderRepository.findByOrderNo(orderNo).orElseGet(() -> {
            BookingOrder order = new BookingOrder();
            order.setOrderNo(orderNo);
            order.setUser(user);
            order.setHomestay(homestay);
            order.setCheckInDate(checkInDate);
            order.setCheckOutDate(checkOutDate);
            order.setNights((int) java.time.temporal.ChronoUnit.DAYS.between(checkInDate, checkOutDate));
            order.setRoomCount(rooms.size());
            order.setTotalAmount(rooms.stream()
                .map(Room::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(BigDecimal.valueOf(order.getNights())));
            order.setOrderStatus(orderStatus);
            order.setPaymentStatus(paymentStatus);
            order.setContactName(contactName);
            order.setContactPhone(contactPhone);
            order.setRemark(remark);
            bookingOrderRepository.save(order);

            for (Room room : rooms) {
                BookingOrderRoom item = new BookingOrderRoom();
                item.setOrder(order);
                item.setRoom(room);
                item.setRoomNo(room.getRoomNo());
                bookingOrderRoomRepository.save(item);
            }
            return order;
        });
    }

    private void ensureReview(
        BookingOrder order,
        User user,
        Homestay homestay,
        int score,
        String content,
        String replyContent,
        ReviewStatus status
    ) {
        if (reviewRepository.findByOrder(order).isPresent()) {
            return;
        }
        Review review = new Review();
        review.setOrder(order);
        review.setUser(user);
        review.setHomestay(homestay);
        review.setScore(score);
        review.setContent(content);
        review.setReplyContent(replyContent);
        review.setStatus(status);
        reviewRepository.save(review);
    }

    private void ensureFavorite(User user, Homestay homestay) {
        if (user == null || favoriteRepository.findByUserAndHomestay(user, homestay).isPresent()) {
            return;
        }
        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setHomestay(homestay);
        favoriteRepository.save(favorite);
    }

    private void refreshHomestayStats(Homestay homestay) {
        long favoriteCount = favoriteRepository.countByHomestay(homestay);
        homestay.setFavoriteCount((int) favoriteCount);
        homestay.setBookingCount(bookingOrderRepository.findByHomestayOrderByCreatedAtDesc(homestay).size());
        List<Review> approvedReviews = reviewRepository.findByHomestayAndStatusOrderByCreatedAtDesc(homestay, ReviewStatus.APPROVED);
        if (approvedReviews.isEmpty()) {
            homestay.setRating(0D);
        } else {
            double avg = approvedReviews.stream().mapToInt(Review::getScore).average().orElse(0D);
            homestay.setRating(BigDecimal.valueOf(avg).setScale(1, java.math.RoundingMode.HALF_UP).doubleValue());
        }
        homestayRepository.save(homestay);
    }

    private Map<String, Object> homestaySummary(Homestay homestay) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", homestay.getId());
        data.put("name", homestay.getName());
        data.put("city", homestay.getCity());
        data.put("district", homestay.getDistrict() == null ? "" : homestay.getDistrict());
        data.put("houseType", homestay.getHouseType());
        data.put("basePrice", homestay.getBasePrice());
        data.put("totalRooms", homestay.getTotalRooms());
        data.put("status", homestay.getStatus().name());
        data.put("hostName", homestay.getHost().getNickname());
        data.put("bookingCount", homestay.getBookingCount());
        data.put("coverImage", homestay.getCoverImage());
        return data;
    }

    private Map<String, Object> homestayEditorData(Homestay homestay) {
        List<HomestayImage> images = homestayImageRepository.findByHomestayOrderBySortOrderAsc(homestay);
        List<Room> rooms = roomRepository.findByHomestayOrderByRoomNoAsc(homestay).stream()
            .filter(room -> Boolean.TRUE.equals(room.getEnabled()))
            .toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", homestay.getId());
        data.put("name", homestay.getName());
        data.put("city", homestay.getCity());
        data.put("district", homestay.getDistrict() == null ? "" : homestay.getDistrict());
        data.put("address", homestay.getAddress());
        data.put("basePrice", homestay.getBasePrice());
        data.put("houseType", homestay.getHouseType());
        data.put("tags", homestay.getTags());
        data.put("facilities", homestay.getFacilities());
        data.put("latitude", homestay.getLatitude());
        data.put("longitude", homestay.getLongitude());
        data.put("coverImage", homestay.getCoverImage());
        data.put("summary", homestay.getSummary());
        data.put("description", homestay.getDescription());
        data.put("status", homestay.getStatus().name());
        data.put("images", images.stream().map(HomestayImage::getImageUrl).toList());
        data.put("rooms", rooms.stream().map(room -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", room.getId());
            item.put("roomNo", room.getRoomNo());
            item.put("roomType", room.getRoomType());
            item.put("floorNo", room.getFloorNo());
            item.put("price", room.getPrice());
            item.put("bedCount", room.getBedCount());
            item.put("capacity", room.getCapacity());
            return item;
        }).toList());
        return data;
    }

    private void applyHomestayBaseInfo(Homestay homestay, HomestaySaveRequest request) {
        homestay.setName(request.name().trim());
        homestay.setCity(request.city().trim());
        homestay.setDistrict(nullableText(request.district()));
        homestay.setAddress(request.address().trim());
        homestay.setBasePrice(request.basePrice());
        homestay.setHouseType(request.houseType().trim());
        homestay.setTags(normalizeCsv(request.tags()));
        homestay.setFacilities(normalizeCsv(request.facilities()));
        homestay.setLatitude(request.latitude());
        homestay.setLongitude(request.longitude());
        homestay.setCoverImage(request.coverImage().trim());
        homestay.setSummary(request.summary().trim());
        homestay.setDescription(request.description().trim());
    }

    private void saveHomestayImages(Homestay homestay, List<String> images) {
        List<String> normalizedImages = images.stream()
            .map(this::nullableText)
            .filter(text -> text != null && !text.isBlank())
            .toList();
        if (normalizedImages.isEmpty()) {
            throw new BusinessException("至少保留一张房源图片");
        }
        homestayImageRepository.deleteAll(homestayImageRepository.findByHomestayOrderBySortOrderAsc(homestay));
        int sortOrder = 1;
        for (String imageUrl : normalizedImages) {
            createImage(homestay, imageUrl, sortOrder++);
        }
    }

    private void syncRooms(Homestay homestay, List<RoomForm> roomForms) {
        if (roomForms == null || roomForms.isEmpty()) {
            throw new BusinessException("请至少配置一个房间");
        }

        List<Room> existingRooms = roomRepository.findByHomestayOrderByRoomNoAsc(homestay);
        Map<Long, Room> existingRoomMap = new LinkedHashMap<>();
        for (Room room : existingRooms) {
            existingRoomMap.put(room.getId(), room);
        }

        Set<Long> retainedRoomIds = new HashSet<>();
        Set<String> roomNoSet = new HashSet<>();

        for (RoomForm roomForm : roomForms) {
            String roomNo = nullableText(roomForm.roomNo());
            String roomType = nullableText(roomForm.roomType());
            if (roomNo == null || roomType == null) {
                throw new BusinessException("房号与房型不能为空");
            }
            String uniqueKey = roomNo.toUpperCase();
            if (!roomNoSet.add(uniqueKey)) {
                throw new BusinessException("房号重复: " + roomNo);
            }

            Room room;
            if (roomForm.id() != null) {
                room = existingRoomMap.get(roomForm.id());
                if (room == null) {
                    throw new BusinessException("房间不存在或不属于当前房源");
                }
            } else {
                room = new Room();
                room.setHomestay(homestay);
            }

            room.setRoomNo(roomNo);
            room.setRoomType(roomType);
            room.setFloorNo(roomForm.floorNo());
            room.setPrice(roomForm.price());
            room.setBedCount(roomForm.bedCount());
            room.setCapacity(roomForm.capacity());
            room.setEnabled(true);
            roomRepository.save(room);
            retainedRoomIds.add(room.getId());
        }

        for (Room room : existingRooms) {
            if (retainedRoomIds.contains(room.getId())) {
                continue;
            }
            if (bookingOrderRoomRepository.existsByRoom(room)) {
                room.setEnabled(false);
                roomRepository.save(room);
            } else {
                roomRepository.delete(room);
            }
        }

        homestay.setTotalRooms(retainedRoomIds.size());
    }

    private boolean shouldOccupyInventory(BookingOrder order) {
        return order.getOrderStatus() == OrderStatus.PENDING_PAYMENT
            || order.getOrderStatus() == OrderStatus.PAID
            || order.getOrderStatus() == OrderStatus.CONFIRMED
            || order.getOrderStatus() == OrderStatus.REFUND_REQUESTED
            || order.getOrderStatus() == OrderStatus.COMPLETED;
    }

    private Homestay loadOwnedHomestay(User operator, Long homestayId) {
        Homestay homestay = homestayRepository.findById(homestayId)
            .orElseThrow(() -> new BusinessException("房源不存在"));
        if (operator.getRole() == RoleType.HOST && !homestay.getHost().getId().equals(operator.getId())) {
            throw new BusinessException("无权管理该房源");
        }
        return homestay;
    }

    private BookingOrder loadOwnedOrder(User operator, Long orderId) {
        BookingOrder order = bookingOrderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException("订单不存在"));
        if (operator.getRole() == RoleType.HOST && !order.getHomestay().getHost().getId().equals(operator.getId())) {
            throw new BusinessException("无权管理该订单");
        }
        return order;
    }

    private void ensureAdmin(User operator) {
        if (operator.getRole() != RoleType.ADMIN) {
            throw new BusinessException("无权操作");
        }
    }

    private Review loadOwnedReview(User operator, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new BusinessException("评论不存在"));
        if (operator.getRole() == RoleType.HOST && !review.getHomestay().getHost().getId().equals(operator.getId())) {
            throw new BusinessException("无权管理该评论");
        }
        return review;
    }

    private List<BookingOrder> filterOrdersForOperator(User operator) {
        if (operator.getRole() != RoleType.HOST) {
            return bookingOrderRepository.findAll();
        }
        return bookingOrderRepository.findAll().stream()
            .filter(order -> order.getHomestay().getHost().getId().equals(operator.getId()))
            .toList();
    }

    private List<Review> filterReviewsForOperator(User operator) {
        if (operator.getRole() != RoleType.HOST) {
            return reviewRepository.findAllByOrderByCreatedAtDesc();
        }
        return reviewRepository.findAllByOrderByCreatedAtDesc().stream()
            .filter(review -> review.getHomestay().getHost().getId().equals(operator.getId()))
            .toList();
    }

    private String normalizeCsv(String source) {
        if (source == null || source.isBlank()) {
            return "";
        }
        List<String> items = new ArrayList<>();
        for (String item : source.split(",")) {
            String text = nullableText(item);
            if (text != null) {
                items.add(text);
            }
        }
        return String.join(",", items);
    }

    private String nullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
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

    private boolean isRemoteUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }
}
