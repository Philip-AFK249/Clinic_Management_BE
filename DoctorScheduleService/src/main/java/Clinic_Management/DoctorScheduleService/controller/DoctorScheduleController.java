package Clinic_Management.DoctorScheduleService.controller;

import Clinic_Management.DoctorScheduleService.dto.*;
import Clinic_Management.DoctorScheduleService.entity.Department;
import Clinic_Management.DoctorScheduleService.entity.Doctor;
import Clinic_Management.DoctorScheduleService.entity.DoctorShift;
import Clinic_Management.DoctorScheduleService.entity.ShiftSession;
import Clinic_Management.DoctorScheduleService.service.DoctorScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
@Tag(name = "Doctor Schedule API", description = "Quản lý lịch trực bác sĩ và slot khám")
public class DoctorScheduleController {

    private final DoctorScheduleService scheduleService;

    @GetMapping("/departments")
    @Operation(summary = "Lấy danh mục tất cả Khoa điều trị")
    public ResponseEntity<List<Department>> getDepartments() {
        return ResponseEntity.ok(scheduleService.getAllDepartments());
    }

    @GetMapping("/doctors")
    @Operation(summary = "Lấy danh sách Bác sĩ trực thuộc Khoa")
    public ResponseEntity<List<Doctor>> getDoctors(@RequestParam Long departmentId) {
        return ResponseEntity.ok(scheduleService.getDoctorsByDepartment(departmentId));
    }

    @GetMapping("/shifts")
    @Operation(summary = "Lấy danh sách phân ca trực theo ngày của Khoa")
    public ResponseEntity<List<DoctorShiftResponse>> getShifts(
            @RequestParam Long departmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(scheduleService.getShiftsByDate(departmentId, date));
    }

    @GetMapping("/shifts/weekly")
    @Operation(summary = "Lấy thời khóa biểu phân ca theo tuần của Khoa")
    public ResponseEntity<List<DoctorShiftResponse>> getWeeklyShifts(
            @RequestParam Long departmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(scheduleService.getWeeklyShifts(departmentId, startDate, endDate));
    }

    @GetMapping("/roster/validate")
    @Operation(summary = "Kiểm tra tuân thủ điều kiện mở ca khám (>= 1 Ngoại trú và >= 1 Nội trú)")
    public ResponseEntity<Boolean> validateRoster(
            @RequestParam Long departmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam ShiftSession session) {
        return ResponseEntity.ok(scheduleService.validateRosterCompliance(departmentId, date, session));
    }

    @PostMapping("/shifts")
    @Operation(summary = "Admin đăng ký ca trực cho bác sĩ (Ngoại trú hoặc Nội trú)")
    public ResponseEntity<DoctorShift> registerShift(@Valid @RequestBody CreateShiftRequest request) {
        return ResponseEntity.ok(scheduleService.registerShift(request));
    }

    @GetMapping("/available-slots")
    @Operation(summary = "Xem các khung giờ khám 60 phút và số chỗ còn trống")
    public ResponseEntity<List<TimeSlotResponse>> getAvailableSlots(
            @RequestParam Long departmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(scheduleService.getAvailableSlots(departmentId, date));
    }


    @PostMapping("/assign-doctor")
    @Operation(summary = "Phân bổ bác sĩ cho bệnh nhân vào slot (Cân bằng tải Least-Busy)")
    public ResponseEntity<AssignDoctorResponse> assignDoctor(@Valid @RequestBody AssignDoctorRequest request) {
        return ResponseEntity.ok(scheduleService.assignDoctorToSlot(request));
    }

    @DeleteMapping("/assignments/{ticketNumber}")
    @Operation(summary = "Giải phóng slot khám khi phiếu khám hoặc lịch hẹn bị hủy")
    public ResponseEntity<Void> releaseSlot(@PathVariable String ticketNumber) {
        scheduleService.releaseSlotAssignment(ticketNumber);
        return ResponseEntity.noContent().build();
    }
}