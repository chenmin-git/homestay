package com.homestay.controller;

import com.homestay.common.ApiResponse;
import com.homestay.dto.OrderDtos.BookingCreateRequest;
import com.homestay.dto.OrderDtos.ReviewCreateRequest;
import com.homestay.dto.UserDtos.ProfileUpdateRequest;
import com.homestay.entity.User;
import com.homestay.repository.UserRepository;
import com.homestay.security.SecurityUtils;
import com.homestay.service.PortalService;
import com.homestay.service.UserCenterService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;
    private final UserCenterService userCenterService;
    private final PortalService portalService;

    public UserController(
        UserRepository userRepository,
        UserCenterService userCenterService,
        PortalService portalService
    ) {
        this.userRepository = userRepository;
        this.userCenterService = userCenterService;
        this.portalService = portalService;
    }

    @GetMapping("/profile")
    public ApiResponse<?> profile() {
        return ApiResponse.ok(userCenterService.profile(currentUser()));
    }

    @PatchMapping("/profile")
    public ApiResponse<?> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.ok("修改成功", userCenterService.updateProfile(currentUser(), request));
    }

    @GetMapping("/favorites")
    public ApiResponse<?> favorites() {
        return ApiResponse.ok(portalService.favorites(currentUser()));
    }

    @PostMapping("/favorites/{homestayId}")
    public ApiResponse<?> toggleFavorite(@PathVariable Long homestayId) {
        return ApiResponse.ok(userCenterService.toggleFavorite(currentUser(), homestayId));
    }

    @GetMapping("/orders")
    public ApiResponse<?> orders() {
        return ApiResponse.ok(userCenterService.orders(currentUser()));
    }

    @PostMapping("/orders")
    public ApiResponse<?> createOrder(@Valid @RequestBody BookingCreateRequest request) {
        return ApiResponse.ok("下单成功", userCenterService.createOrder(currentUser(), request));
    }

    @PostMapping("/orders/{orderId}/pay")
    public ApiResponse<?> payOrder(@PathVariable Long orderId) {
        return ApiResponse.ok("支付成功", userCenterService.payOrder(currentUser(), orderId));
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ApiResponse<?> cancelOrder(@PathVariable Long orderId) {
        return ApiResponse.ok("订单已取消", userCenterService.cancelOrder(currentUser(), orderId));
    }

    @PostMapping("/orders/{orderId}/refund")
    public ApiResponse<?> refundOrder(@PathVariable Long orderId) {
        return ApiResponse.ok("退款申请已提交", userCenterService.refundOrder(currentUser(), orderId));
    }

    @PostMapping("/orders/{orderId}/complete")
    public ApiResponse<?> completeOrder(@PathVariable Long orderId) {
        return ApiResponse.ok("订单已完成", userCenterService.completeOrder(currentUser(), orderId));
    }

    @PostMapping("/reviews")
    public ApiResponse<?> review(@Valid @RequestBody ReviewCreateRequest request) {
        return ApiResponse.ok("评价成功", userCenterService.createReview(currentUser(), request));
    }

    private User currentUser() {
        Long userId = SecurityUtils.currentUser().userId();
        return userRepository.findById(userId).orElseThrow();
    }
}
