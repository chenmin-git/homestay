package com.homestay.controller;

import com.homestay.common.ApiResponse;
import com.homestay.security.JwtUserPrincipal;
import com.homestay.service.PortalService;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final PortalService portalService;

    @GetMapping("/home")
    public ApiResponse<?> home() {
        return ApiResponse.ok(portalService.homeData());
    }

    @GetMapping("/homestays")
    public ApiResponse<?> homestays(
        @RequestParam(required = false) String city,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) String houseType,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "8") int size
    ) {
        return ApiResponse.ok(portalService.search(city, keyword, minPrice, maxPrice, houseType, page, size, currentUserId()));
    }

    @GetMapping("/homestays/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        return ApiResponse.ok(portalService.homestayDetail(id, currentUserId()));
    }

    @GetMapping("/homestays/{id}/availability")
    public ApiResponse<?> availability(
        @PathVariable Long id,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkInDate,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate checkOutDate
    ) {
        return ApiResponse.ok(portalService.availableRooms(id, checkInDate, checkOutDate));
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtUserPrincipal principal) {
            return principal.userId();
        }
        return null;
    }
}
