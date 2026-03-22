package com.homestay.controller;

import com.homestay.common.ApiResponse;
import com.homestay.dto.AdminDtos.BannerSaveRequest;
import com.homestay.dto.AdminDtos.HomestaySaveRequest;
import com.homestay.dto.AdminDtos.NoticeSaveRequest;
import com.homestay.dto.AdminDtos.PasswordChangeRequest;
import com.homestay.dto.AdminDtos.ReviewReplyRequest;
import com.homestay.entity.User;
import com.homestay.repository.UserRepository;
import com.homestay.security.SecurityUtils;
import com.homestay.service.AdminService;
import javax.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','HOST')")
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;

    public AdminController(AdminService adminService, UserRepository userRepository) {
        this.adminService = adminService;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public ApiResponse<?> dashboard() {
        return ApiResponse.ok(adminService.dashboard(currentUser()));
    }

    @GetMapping("/homestays")
    public ApiResponse<?> homestays() {
        return ApiResponse.ok(adminService.homestays(currentUser()));
    }

    @PostMapping("/homestays")
    public ApiResponse<?> createHomestay(@Valid @RequestBody HomestaySaveRequest request) {
        return ApiResponse.ok("房源发布成功", adminService.createHomestay(currentUser(), request));
    }

    @GetMapping("/homestays/{homestayId}")
    public ApiResponse<?> homestayDetail(@PathVariable("homestayId") Long homestayId) {
        return ApiResponse.ok(adminService.homestayDetail(currentUser(), homestayId));
    }

    @PutMapping("/homestays/{homestayId}")
    public ApiResponse<?> updateHomestay(@PathVariable("homestayId") Long homestayId, @Valid @RequestBody HomestaySaveRequest request) {
        return ApiResponse.ok("房源更新成功", adminService.updateHomestay(currentUser(), homestayId, request));
    }

    @PostMapping("/homestays/{homestayId}/toggle-status")
    public ApiResponse<?> toggleHomestayStatus(@PathVariable("homestayId") Long homestayId) {
        return ApiResponse.ok(adminService.toggleHomestayStatus(currentUser(), homestayId));
    }

    @DeleteMapping("/homestays/{homestayId}")
    public ApiResponse<?> deleteHomestay(@PathVariable("homestayId") Long homestayId) {
        return ApiResponse.ok("房源删除成功", adminService.deleteHomestay(currentUser(), homestayId));
    }

    @GetMapping("/homestays/{homestayId}/calendar")
    public ApiResponse<?> homestayCalendar(
        @PathVariable("homestayId") Long homestayId,
        @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @RequestParam(value = "days", defaultValue = "7") int days
    ) {
        return ApiResponse.ok(adminService.homestayCalendar(currentUser(), homestayId, startDate, days));
    }

    @GetMapping("/orders")
    public ApiResponse<?> orders() {
        return ApiResponse.ok(adminService.orders(currentUser()));
    }

    @PostMapping("/orders/{orderId}/confirm")
    @PreAuthorize("hasRole('HOST')")
    public ApiResponse<?> confirmOrder(@PathVariable("orderId") Long orderId) {
        return ApiResponse.ok("已确认入住", adminService.confirmOrder(currentUser(), orderId));
    }

    @PostMapping("/orders/{orderId}/refund")
    @PreAuthorize("hasRole('HOST')")
    public ApiResponse<?> refundOrder(@PathVariable("orderId") Long orderId) {
        return ApiResponse.ok("退款处理完成", adminService.refundOrder(currentUser(), orderId));
    }

    @DeleteMapping("/orders/{orderId}")
    public ApiResponse<?> deleteOrder(@PathVariable("orderId") Long orderId) {
        adminService.deleteOrder(currentUser(), orderId);
        return ApiResponse.ok("订单删除成功", null);
    }

    @GetMapping("/orders/export")
    public ResponseEntity<byte[]> exportOrders() throws IOException {
        List<?> rows = adminService.orders(currentUser());
        String[] headers = { "订单号", "下单时间", "用户", "民宿", "入住日期", "退房日期", "金额", "订单状态", "支付状态" };
        String sheetName = "订单列表";

        HSSFWorkbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);
        int rowIndex = 0;
        Row headerRow = sheet.createRow(rowIndex++);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        for (Object row : rows) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> item = (java.util.Map<String, Object>) row;
            Row dataRow = sheet.createRow(rowIndex++);
            dataRow.createCell(0).setCellValue(safeText(item.get("orderNo")));
            dataRow.createCell(1).setCellValue(safeText(item.get("createdAt")));
            dataRow.createCell(2).setCellValue(safeText(item.get("username")));
            dataRow.createCell(3).setCellValue(safeText(item.get("homestayName")));
            dataRow.createCell(4).setCellValue(safeText(item.get("checkInDate")));
            dataRow.createCell(5).setCellValue(safeText(item.get("checkOutDate")));
            dataRow.createCell(6).setCellValue(safeText(item.get("totalAmount")));
            dataRow.createCell(7).setCellValue(formatOrderStatus(item.get("orderStatus")));
            dataRow.createCell(8).setCellValue(formatPaymentStatus(item.get("paymentStatus")));
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            workbook.close();
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=%E8%AE%A2%E5%8D%95%E5%88%97%E8%A1%A8.xls")
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(outputStream.toByteArray());
        }
    }

    private String formatOrderStatus(Object status) {
        String value = safeText(status).toUpperCase(Locale.ROOT);
        switch (value) {
            case "PENDING_PAYMENT": return "待支付";
            case "PAID": return "已支付";
            case "CONFIRMED": return "待入住";
            case "COMPLETED": return "已完成";
            case "CANCELLED": return "已取消";
            case "REFUND_REQUESTED": return "退款中";
            case "REFUNDED": return "已退款";
            default: return safeText(status);
        }
    }

    private String formatPaymentStatus(Object status) {
        String value = safeText(status).toUpperCase(Locale.ROOT);
        switch (value) {
            case "PAID": return "已支付";
            case "UNPAID": return "未支付";
            case "REFUNDED": return "已退款";
            default: return safeText(status);
        }
    }

    private String safeText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> users() {
        return ApiResponse.ok(adminService.users());
    }

    @GetMapping("/host-applications")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> hostApplications() {
        return ApiResponse.ok(adminService.hostApplications(currentUser()));
    }

    @GetMapping("/password-reset-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> passwordResetRequests() {
        return ApiResponse.ok(adminService.passwordResetRequests(currentUser()));
    }

    @PostMapping("/host-applications/{applicationId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> approveHostApplication(@PathVariable("applicationId") Long applicationId) {
        return ApiResponse.ok("审核通过", adminService.approveHostApplication(currentUser(), applicationId));
    }

    @PostMapping("/host-applications/{applicationId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> rejectHostApplication(@PathVariable("applicationId") Long applicationId) {
        return ApiResponse.ok("已拒绝", adminService.rejectHostApplication(currentUser(), applicationId));
    }

    @PostMapping("/password-reset-requests/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> approvePasswordResetRequest(@PathVariable("requestId") Long requestId) {
        return ApiResponse.ok("改密审核通过", adminService.approvePasswordResetRequest(currentUser(), requestId));
    }

    @PostMapping("/password-reset-requests/{requestId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> rejectPasswordResetRequest(@PathVariable("requestId") Long requestId) {
        return ApiResponse.ok("改密申请已拒绝", adminService.rejectPasswordResetRequest(currentUser(), requestId));
    }

    @PostMapping("/users/{userId}/toggle-enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> toggleEnabled(@PathVariable("userId") Long userId) {
        return ApiResponse.ok(adminService.toggleUserStatus(userId));
    }

    @PostMapping("/users/{userId}/toggle-blacklist")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> toggleBlacklist(@PathVariable("userId") Long userId) {
        return ApiResponse.ok(adminService.toggleBlacklist(userId));
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> deleteUser(@PathVariable("userId") Long userId) {
        adminService.deleteUser(currentUser(), userId);
        return ApiResponse.ok("用户删除成功", null);
    }

    @GetMapping("/reviews")
    public ApiResponse<?> reviews() {
        return ApiResponse.ok(adminService.reviews(currentUser()));
    }

    @PostMapping("/reviews/{reviewId}/reply")
    public ApiResponse<?> replyReview(@PathVariable("reviewId") Long reviewId, @Valid @RequestBody ReviewReplyRequest request) {
        return ApiResponse.ok(adminService.replyReview(currentUser(), reviewId, request.getReplyContent()));
    }

    @PostMapping("/reviews/{reviewId}/hide")
    public ApiResponse<?> hideReview(@PathVariable("reviewId") Long reviewId) {
        adminService.hideReview(currentUser(), reviewId);
        return ApiResponse.ok("评论已隐藏", null);
    }

    @GetMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> settings() {
        return ApiResponse.ok(adminService.settings());
    }

    @PutMapping("/banners")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> updateBanners(@Valid @RequestBody List<BannerSaveRequest> requests) {
        adminService.updateBanners(requests);
        return ApiResponse.ok("轮播图更新成功", null);
    }

    @PutMapping("/notices")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> updateNotices(@Valid @RequestBody List<NoticeSaveRequest> requests) {
        adminService.updateNotices(requests);
        return ApiResponse.ok("公告更新成功", null);
    }

    @PostMapping("/password")
    public ApiResponse<?> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        adminService.changePassword(currentUser(), request);
        return ApiResponse.ok("密码修改成功", null);
    }

    private User currentUser() {
        Long userId = SecurityUtils.currentUser().getUserId();
        return userRepository.findById(userId).orElseThrow();
    }
}
