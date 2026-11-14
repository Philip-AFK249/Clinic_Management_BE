package Clinic_Management.ClinicalConsultationService.controller;

import Clinic_Management.ClinicalConsultationService.dto.CompleteEncounterRequest;
import Clinic_Management.ClinicalConsultationService.dto.EncounterResponse;
import Clinic_Management.ClinicalConsultationService.dto.StartEncounterRequest;
import Clinic_Management.ClinicalConsultationService.service.ConsultationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clinical/encounters")
@RequiredArgsConstructor
@Tag(name = "Clinical Consultation API", description = "Dành cho Bác sĩ: Khám bệnh, nhập hồ sơ SOAP, chẩn đoán ICD-10 và kê đơn")
public class ConsultationController {

    private final ConsultationService consultationService;

    @PostMapping("/start")
    @Operation(summary = "Bác sĩ bấm tiếp nhận bệnh nhân vào khám (Chuyển trạng thái ticket sang IN_CONSULTATION)")
    public ResponseEntity<EncounterResponse> startEncounter(@Valid @RequestBody StartEncounterRequest request) {
        return ResponseEntity.ok(consultationService.startEncounter(request));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Bác sĩ ký duyệt hoàn tất ca khám, đẩy đơn thuốc sang Dược và hoàn tất ticket")
    public ResponseEntity<EncounterResponse> completeEncounter(
            @PathVariable Long id,
            @Valid @RequestBody CompleteEncounterRequest request) {
        return ResponseEntity.ok(consultationService.completeEncounter(id, request));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Lấy toàn bộ lịch sử bệnh án, chẩn đoán ICD-10, đơn thuốc và chi phí của bệnh nhân")
    public ResponseEntity<List<EncounterResponse>> getPatientHistory(@PathVariable Long patientId) {
        return ResponseEntity.ok(consultationService.getPatientHistory(patientId));
    }

    @GetMapping("/by-tickets")
    @Operation(summary = "Tra cứu lịch sử bệnh án và chi phí theo danh sách mã phiếu khám")
    public ResponseEntity<List<EncounterResponse>> getEncountersByTickets(
            @RequestParam List<String> ticketNumbers) {
        return ResponseEntity.ok(consultationService.getEncountersByTicketNumbers(ticketNumbers));
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Lấy toàn bộ lịch sử ca khám và danh sách bệnh nhân do Bác sĩ đã từng khám")
    public ResponseEntity<List<EncounterResponse>> getDoctorEncounters(@PathVariable Long doctorId) {
        return ResponseEntity.ok(consultationService.getDoctorEncounters(doctorId));
    }
}