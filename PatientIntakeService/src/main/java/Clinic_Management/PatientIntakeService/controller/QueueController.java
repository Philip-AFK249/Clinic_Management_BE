package Clinic_Management.PatientIntakeService.controller;
import Clinic_Management.PatientIntakeService.dto.QueueTicketDetailResponse;
import Clinic_Management.PatientIntakeService.entity.QueueStatus;
import Clinic_Management.PatientIntakeService.entity.QueueTicket;
import Clinic_Management.PatientIntakeService.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
@Tag(name = "Queue Management API", description = "Quản lý và điều phối hàng đợi khám bệnh")
public class QueueController {

    private final QueueService queueService;

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Lấy danh sách hàng đợi đang hoạt động của Bác sĩ (ưu tiên P1 -> slotStartTime -> P2/P3)")
    public ResponseEntity<List<QueueTicket>> getDoctorQueue(
            @PathVariable Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(queueService.getDoctorActiveQueue(doctorId, targetDate));
    }

    @PostMapping("/doctor/{doctorId}/call-next")
    @Operation(summary = "Bác sĩ gọi bệnh nhân kế tiếp vào phòng (tự động chặn nếu đang có ca dở dang)")
    public ResponseEntity<QueueTicket> callNext(
            @PathVariable Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(queueService.callNextPatient(doctorId, targetDate));
    }

    @GetMapping("/tickets/{ticketNumber}")
    @Operation(summary = "Lấy thông tin chi tiết phiếu khám kèm hồ sơ bệnh nhân (Dành cho màn hình EHR Bác sĩ)")
    public ResponseEntity<QueueTicketDetailResponse> getTicketDetails(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(queueService.getTicketDetails(ticketNumber));
    }

    @PatchMapping("/tickets/{ticketNumber}/status")
    @Operation(summary = "Cập nhật trạng thái phiếu (IN_CONSULTATION, COMPLETED, SKIPPED, CANCELLED)")
    public ResponseEntity<QueueTicket> updateStatus(
            @PathVariable String ticketNumber,
            @RequestParam QueueStatus status) {
        return ResponseEntity.ok(queueService.updateTicketStatus(ticketNumber, status));
    }

    @GetMapping("/reception")
    @Operation(summary = "Lấy danh sách hàng đợi trực tiếp cho Quầy tiếp đón")
    public ResponseEntity<List<QueueTicket>> getReceptionQueue(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(queueService.getReceptionQueue(departmentId, targetDate));
    }

    @GetMapping("/patient/{patientId}/active")
    @Operation(summary = "Lấy phiếu khám đang hoạt động gần nhất của bệnh nhân")
    public ResponseEntity<QueueTicketDetailResponse> getActivePatientTicket(@PathVariable Long patientId) {
        return ResponseEntity.ok(queueService.getActiveTicketByPatientId(patientId));
    }

    @GetMapping("/patient/active")
    public ResponseEntity<QueueTicketDetailResponse> getActivePatientTicket(
            @RequestParam(required = false) String identifier) {
        return ResponseEntity.ok(queueService.getActiveTicketByIdentifier(identifier));
    }
}