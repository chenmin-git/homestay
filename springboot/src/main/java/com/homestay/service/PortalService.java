package com.homestay.service;

import com.homestay.common.BusinessException;
import com.homestay.entity.Banner;
import com.homestay.entity.BookingOrder;
import com.homestay.entity.BookingOrderRoom;
import com.homestay.entity.Favorite;
import com.homestay.entity.Homestay;
import com.homestay.entity.HomestayImage;
import com.homestay.entity.Review;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.enums.HomestayStatus;
import com.homestay.enums.OrderStatus;
import com.homestay.enums.ReviewStatus;
import com.homestay.repository.BannerRepository;
import com.homestay.repository.BookingOrderRepository;
import com.homestay.repository.BookingOrderRoomRepository;
import com.homestay.repository.FavoriteRepository;
import com.homestay.repository.HomestayImageRepository;
import com.homestay.repository.HomestayRepository;
import com.homestay.repository.NoticeRepository;
import com.homestay.repository.ReviewRepository;
import com.homestay.repository.RoomRepository;
import com.homestay.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalService {

    private static final List<OrderStatus> ACTIVE_ORDER_STATUSES = List.of(
        OrderStatus.PENDING_PAYMENT,
        OrderStatus.PAID,
        OrderStatus.CONFIRMED,
        OrderStatus.COMPLETED
    );

    private final BannerRepository bannerRepository;
    private final NoticeRepository noticeRepository;
    private final HomestayRepository homestayRepository;
    private final HomestayImageRepository homestayImageRepository;
    private final ReviewRepository reviewRepository;
    private final RoomRepository roomRepository;
    private final BookingOrderRepository bookingOrderRepository;
    private final BookingOrderRoomRepository bookingOrderRoomRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;

    public PortalService(
        BannerRepository bannerRepository,
        NoticeRepository noticeRepository,
        HomestayRepository homestayRepository,
        HomestayImageRepository homestayImageRepository,
        ReviewRepository reviewRepository,
        RoomRepository roomRepository,
        BookingOrderRepository bookingOrderRepository,
        BookingOrderRoomRepository bookingOrderRoomRepository,
        FavoriteRepository favoriteRepository,
        UserRepository userRepository
    ) {
        this.bannerRepository = bannerRepository;
        this.noticeRepository = noticeRepository;
        this.homestayRepository = homestayRepository;
        this.homestayImageRepository = homestayImageRepository;
        this.reviewRepository = reviewRepository;
        this.roomRepository = roomRepository;
        this.bookingOrderRepository = bookingOrderRepository;
        this.bookingOrderRoomRepository = bookingOrderRoomRepository;
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
    }

    public Map<String, Object> homeData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("banners", bannerRepository.findByEnabledTrueOrderBySortOrderAsc().stream().map(this::bannerSummary).toList());
        data.put("notices", noticeRepository.findTop5ByPublishedTrueOrderByCreatedAtDesc());
        data.put("hotHomestays", homestayRepository.findTop6ByStatusOrderByBookingCountDescCreatedAtDesc(HomestayStatus.ONLINE)
            .stream().map(h -> homestayCard(h, null)).toList());
        data.put("latestHomestays", homestayRepository.findTop6ByStatusOrderByCreatedAtDesc(HomestayStatus.ONLINE)
            .stream().map(h -> homestayCard(h, null)).toList());
        data.put("houseTypes", List.of("大床房", "双床房", "亲子房", "Loft", "整栋别墅"));
        return data;
    }

    public Map<String, Object> search(
        String city,
        String keyword,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String houseType,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int page,
        int size,
        Long userId
    ) {
        String normalizedCity = emptyToNull(city);
        String normalizedKeyword = emptyToNull(keyword);
        String normalizedHouseType = emptyToNull(houseType);
        int pageNumber = Math.max(page, 0);
        int pageSize = Math.max(size, 1);
        User user = loadUserNullable(userId);

        if (checkInDate != null && checkOutDate != null) {
            if (!checkOutDate.isAfter(checkInDate)) {
                throw new BusinessException("退房日期必须晚于入住日期");
            }
            if (checkInDate.isBefore(LocalDate.now())) {
                throw new BusinessException("入住日期不能早于今天");
            }

            List<Map<String, Object>> matched = homestayRepository.searchAll(
                HomestayStatus.ONLINE,
                normalizedCity,
                normalizedKeyword,
                minPrice,
                maxPrice,
                normalizedHouseType
            ).stream()
                .map(homestay -> homestayCard(homestay, user, countAvailableRooms(homestay, checkInDate, checkOutDate)))
                .filter(item -> Integer.parseInt(String.valueOf(item.get("availableRoomCount"))) > 0)
                .toList();

            int fromIndex = Math.min(pageNumber * pageSize, matched.size());
            int toIndex = Math.min(fromIndex + pageSize, matched.size());
            return Map.of(
                "content", matched.subList(fromIndex, toIndex),
                "page", pageNumber,
                "size", pageSize,
                "total", matched.size()
            );
        }

        var result = homestayRepository.search(
            HomestayStatus.ONLINE,
            normalizedCity,
            normalizedKeyword,
            minPrice,
            maxPrice,
            normalizedHouseType,
            PageRequest.of(pageNumber, pageSize)
        );
        return Map.of(
            "content", result.getContent().stream().map(h -> homestayCard(h, user, null)).toList(),
            "page", result.getNumber(),
            "size", result.getSize(),
            "total", result.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> homestayDetail(Long homestayId, Long userId) {
        Homestay homestay = homestayRepository.findById(homestayId)
            .orElseThrow(() -> new BusinessException("房源不存在"));
        User user = loadUserNullable(userId);
        List<HomestayImage> images = homestayImageRepository.findByHomestayOrderBySortOrderAsc(homestay);
        List<Review> reviews = reviewRepository.findByHomestayAndStatusOrderByCreatedAtDesc(homestay, ReviewStatus.APPROVED);
        List<Room> rooms = roomRepository.findByHomestayAndEnabledTrueOrderByRoomNoAsc(homestay);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", homestay.getId());
        data.put("name", homestay.getName());
        data.put("city", homestay.getCity());
        data.put("district", homestay.getDistrict());
        data.put("address", homestay.getAddress());
        data.put("summary", homestay.getSummary());
        data.put("description", homestay.getDescription());
        data.put("basePrice", homestay.getBasePrice());
        data.put("totalRooms", homestay.getTotalRooms());
        data.put("houseType", homestay.getHouseType());
        data.put("tags", splitCsv(homestay.getTags()));
        data.put("facilities", splitCsv(homestay.getFacilities()));
        data.put("latitude", homestay.getLatitude());
        data.put("longitude", homestay.getLongitude());
        data.put("coverImage", homestay.getCoverImage());
        data.put("rating", homestay.getRating());
        data.put("favoriteCount", homestay.getFavoriteCount());
        data.put("bookingCount", homestay.getBookingCount());
        data.put("host", Map.of(
            "id", homestay.getHost().getId(),
            "nickname", homestay.getHost().getNickname(),
            "phone", homestay.getHost().getPhone() == null ? "" : homestay.getHost().getPhone()
        ));
        data.put("images", images.stream().map(HomestayImage::getImageUrl).toList());
        data.put("rooms", rooms.stream().map(room -> Map.of(
            "id", room.getId(),
            "roomNo", room.getRoomNo(),
            "roomType", room.getRoomType(),
            "price", room.getPrice(),
            "floorNo", room.getFloorNo(),
            "bedCount", room.getBedCount(),
            "capacity", room.getCapacity()
        )).toList());
        data.put("reviews", reviews.stream().map(this::reviewSummary).toList());
        data.put("favorite", user != null && favoriteRepository.findByUserAndHomestay(user, homestay).isPresent());
        return data;
    }

    public List<Map<String, Object>> availableRooms(Long homestayId, LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null || !checkOutDate.isAfter(checkInDate)) {
            throw new BusinessException("入住与退房日期不合法");
        }
        if (checkInDate.isBefore(LocalDate.now())) {
            throw new BusinessException("入住日期不能早于今天");
        }
        Homestay homestay = homestayRepository.findById(homestayId)
            .orElseThrow(() -> new BusinessException("房源不存在"));
        List<Room> rooms = roomRepository.findByHomestayAndEnabledTrueOrderByRoomNoAsc(homestay);
        List<Long> roomIds = rooms.stream().map(Room::getId).toList();
        List<BookingOrder> conflicts = bookingOrderRepository.findConflictingOrders(roomIds, checkInDate, checkOutDate, ACTIVE_ORDER_STATUSES);
        List<Long> unavailableRoomIds = conflicts.stream()
            .flatMap(order -> bookingOrderRoomRepository.findByOrder(order).stream())
            .map(item -> item.getRoom().getId())
            .distinct()
            .toList();
        return rooms.stream()
            .filter(room -> !unavailableRoomIds.contains(room.getId()))
            .map(room -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", room.getId());
                item.put("roomNo", room.getRoomNo());
                item.put("roomType", room.getRoomType());
                item.put("price", room.getPrice());
                item.put("capacity", room.getCapacity());
                item.put("floorNo", room.getFloorNo());
                return item;
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> favorites(User user) {
        List<Map<String, Object>> items = favoriteRepository.findByUserOrderByCreatedAtDesc(user).stream()
            .map(Favorite::getHomestay)
            .map(h -> homestayCard(h, user, null))
            .toList();
        return Map.of("content", items);
    }

    public Map<String, Object> homestayCard(Homestay homestay, User user) {
        return homestayCard(homestay, user, null);
    }

    public Map<String, Object> homestayCard(Homestay homestay, User user, Integer availableRoomCount) {
        boolean favorite = user != null && favoriteRepository.findByUserAndHomestay(user, homestay).isPresent();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", homestay.getId());
        data.put("name", homestay.getName());
        data.put("city", homestay.getCity());
        data.put("district", homestay.getDistrict() == null ? "" : homestay.getDistrict());
        data.put("basePrice", homestay.getBasePrice());
        data.put("houseType", homestay.getHouseType());
        data.put("tags", splitCsv(homestay.getTags()));
        data.put("coverImage", homestay.getCoverImage());
        data.put("rating", homestay.getRating());
        data.put("favoriteCount", homestay.getFavoriteCount());
        data.put("bookingCount", homestay.getBookingCount());
        data.put("favorite", favorite);
        data.put("summary", homestay.getSummary());
        data.put("availableRoomCount", availableRoomCount);
        return data;
    }

    public void recalculateHomestayRating(Homestay homestay) {
        List<Review> reviews = reviewRepository.findByHomestayAndStatusOrderByCreatedAtDesc(homestay, ReviewStatus.APPROVED);
        if (reviews.isEmpty()) {
            homestay.setRating(0D);
        } else {
            double avg = reviews.stream().mapToInt(Review::getScore).average().orElse(0D);
            homestay.setRating(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue());
        }
        homestayRepository.save(homestay);
    }

    private User loadUserNullable(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    private int countAvailableRooms(Homestay homestay, LocalDate checkInDate, LocalDate checkOutDate) {
        List<Room> rooms = roomRepository.findByHomestayAndEnabledTrueOrderByRoomNoAsc(homestay);
        if (rooms.isEmpty()) {
            return 0;
        }
        List<Long> roomIds = rooms.stream().map(Room::getId).toList();
        List<BookingOrder> conflicts = bookingOrderRepository.findConflictingOrders(roomIds, checkInDate, checkOutDate, ACTIVE_ORDER_STATUSES);
        List<Long> unavailableRoomIds = conflicts.stream()
            .flatMap(order -> bookingOrderRoomRepository.findByOrder(order).stream())
            .map(item -> item.getRoom().getId())
            .distinct()
            .toList();
        return (int) rooms.stream().filter(room -> !unavailableRoomIds.contains(room.getId())).count();
    }

    private Map<String, Object> reviewSummary(Review review) {
        return Map.of(
            "id", review.getId(),
            "score", review.getScore(),
            "content", review.getContent() == null ? "" : review.getContent(),
            "imageUrls", splitCsv(review.getImageUrls()),
            "replyContent", review.getReplyContent() == null ? "" : review.getReplyContent(),
            "createdAt", review.getCreatedAt(),
            "nickname", review.getUser().getNickname()
        );
    }

    private Map<String, Object> bannerSummary(Banner banner) {
        return Map.of(
            "id", banner.getId(),
            "title", banner.getTitle(),
            "imageUrl", banner.getImageUrl(),
            "linkUrl", banner.getLinkUrl() == null ? "" : banner.getLinkUrl()
        );
    }

    private List<String> splitCsv(String source) {
        if (source == null || source.isBlank()) {
            return List.of();
        }
        List<String> items = new ArrayList<>();
        for (String item : source.split(",")) {
            if (!item.isBlank()) {
                items.add(item.trim());
            }
        }
        return items;
    }

    private String emptyToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
