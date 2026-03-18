package com.homestay.controller;

import com.homestay.common.ApiResponse;
import com.homestay.dto.AdminDtos.HomestaySaveRequest;
import com.homestay.dto.AdminDtos.ReviewReplyRequest;
import com.homestay.entity.User;
import com.homestay.repository.UserRepository;
import com.homestay.security.SecurityUtils;
import com.homestay.service.AdminService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','HOST')")
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public ApiResponse<?> dashboard() {
        return ApiResponse.ok(adminService.dashboard());
    }

    @GetMapping("/homestays")
    public ApiResponse<?> homestays() {
        return ApiResponse.ok(adminService.homestays(currentUser()));
    }

    @PostMapping("/homestays")
    public ApiResponse<?> createHomestay(@Valid @RequestBody HomestaySaveRequest request) {
        return ApiResponse.ok("房源发布成功", adminService.createHomestay(currentUser(), request));
    }

    @GetMapping("/orders")
    public ApiResponse<?> orders() {
        return ApiResponse.ok(adminService.orders());
    }

    @PostMapping("/orders/{orderId}/confirm")
    public ApiResponse<?> confirmOrder(@PathVariable Long orderId) {
        return ApiResponse.ok("已确认入住", adminService.confirmOrder(orderId));
    }

    @PostMapping("/orders/{orderId}/refund")
    public ApiResponse<?> refundOrder(@PathVariable Long orderId) {
        return ApiResponse.ok("退款处理完成", adminService.refundOrder(orderId));
    }

    @GetMapping("/orders/export")
    public ResponseEntity<byte[]> exportOrders() {
        List<?> rows = adminService.orders();
        StringBuilder builder = new StringBuilder("订单号,用户,民宿,入住日期,退房日期,金额,订单状态,支付状态\n");
        rows.forEach(row -> {
            @SuppressWarnings("unchecked")
            var item = (java.util.Map<String, Object>) row;
            builder.append(item.get("orderNo")).append(',')
                .append(item.get("username")).append(',')
                .append(item.get("homestayName")).append(',')
                .append(item.get("checkInDate")).append(',')
                .append(item.get("checkOutDate")).append(',')
                .append(item.get("totalAmount")).append(',')
                .append(item.get("orderStatus")).append(',')
                .append(item.get("paymentStatus")).append('\n');
        });
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.csv")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> users() {
        return ApiResponse.ok(adminService.users());
    }

    @PostMapping("/users/{userId}/toggle-enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> toggleEnabled(@PathVariable Long userId) {
        return ApiResponse.ok(adminService.toggleUserStatus(userId));
    }

    @PostMapping("/users/{userId}/toggle-blacklist")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> toggleBlacklist(@PathVariable Long userId) {
        return ApiResponse.ok(adminService.toggleBlacklist(userId));
    }

    @GetMapping("/reviews")
    public ApiResponse<?> reviews() {
        return ApiResponse.ok(adminService.reviews());
    }

    @PostMapping("/reviews/{reviewId}/reply")
    public ApiResponse<?> replyReview(@PathVariable Long reviewId, @Valid @RequestBody ReviewReplyRequest request) {
        return ApiResponse.ok(adminService.replyReview(reviewId, request.replyContent()));
    }

    @PostMapping("/reviews/{reviewId}/hide")
    public ApiResponse<?> hideReview(@PathVariable Long reviewId) {
        adminService.hideReview(reviewId);
        return ApiResponse.ok("评论已隐藏", null);
    }

    @GetMapping("/settings")
    public ApiResponse<?> settings() {
        return ApiResponse.ok(adminService.settings());
    }

    private User currentUser() {
        Long userId = SecurityUtils.currentUser().userId();
        return userRepository.findById(userId).orElseThrow();
    }
}
