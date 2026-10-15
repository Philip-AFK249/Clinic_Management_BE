package Clinic_Management.AuthService.controller;

import Clinic_Management.AuthService.dto.*;
import Clinic_Management.AuthService.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & User API", description = "Xác thực tập trung, Cấp phát JWT và Quản lý tài khoản")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập hệ thống (Xác thực Email/Password, trả về JWT Token và User Context)")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Bệnh nhân tự tạo tài khoản cá nhân trực tuyến")
    public ResponseEntity<AuthResponse> registerPatient(@Valid @RequestBody RegisterPatientRequest request) {
        return ResponseEntity.ok(authService.registerPatient(request));
    }

    @PostMapping("/admin/users")
    @Operation(summary = "Quản trị viên cấp tài khoản mới cho Bác sĩ, Điều dưỡng, Lễ tân, Dược sĩ")
    public ResponseEntity<AuthResponse> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.ok(authService.createStaffAccount(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin người dùng từ JWT Token trong Header Authorization")
    public ResponseEntity<AuthResponse> getMyProfile(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(authService.getProfileByToken(authHeader));
    }
    @GetMapping("/admin/patients")
    @Operation(summary = "Admin lấy danh sách toàn bộ Bệnh nhân kèm thông tin BHYT")
    public ResponseEntity<List<AuthResponse>> getAllPatients() {
        return ResponseEntity.ok(authService.getAllPatients());
    }

    @PutMapping("/admin/patients/{id}")
    @Operation(summary = "Admin cập nhật thông tin định danh và BHYT của Bệnh nhân")
    public ResponseEntity<AuthResponse> updatePatientByAdmin(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePatientRequest request) {
        return ResponseEntity.ok(authService.updatePatientByAdmin(id, request));
    }

    @PutMapping("/me/profile")
    @Operation(summary = "Người dùng tự cập nhật hồ sơ cá nhân và BHYT")
    public ResponseEntity<AuthResponse> updateMyProfile(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdatePatientRequest request) {
        return ResponseEntity.ok(authService.updateMyProfile(authHeader, request));
    }

    @GetMapping("/admin/users")
    @Operation(summary = "Lấy danh sách tất cả nhân sự cho Admin")
    public ResponseEntity<List<AuthResponse>> getAllStaff() {
        return ResponseEntity.ok(authService.getAllStaff());
    }

    @PatchMapping("/admin/users/{id}/toggle-status")
    @Operation(summary = "Admin khóa hoặc mở khóa tài khoản nhân sự")
    public ResponseEntity<AuthResponse> toggleUserStatus(@PathVariable Long id) {
        return ResponseEntity.ok(authService.toggleUserStatus(id));
    }

    @PostMapping("/admin/users/{id}/reset-password")
    @Operation(summary = "Admin đặt lại mật khẩu nhân sự về mặc định (password123)")
    public ResponseEntity<Map<String, String>> resetStaffPassword(@PathVariable Long id) {
        authService.resetStaffPassword(id);
        return ResponseEntity.ok(Map.of("message", "Đã đặt lại mật khẩu về 'password123' thành công"));
    }

    @DeleteMapping("/admin/patients/{id}")
    @Operation(summary = "Admin xóa tài khoản và thông tin Bệnh nhân")
    public ResponseEntity<Map<String, String>> deletePatientByAdmin(@PathVariable Long id) {
        authService.deletePatientByAdmin(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa hồ sơ bệnh nhân thành công"));
    }
}