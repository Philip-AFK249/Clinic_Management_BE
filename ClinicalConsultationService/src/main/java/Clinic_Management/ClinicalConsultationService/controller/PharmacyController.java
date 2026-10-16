package Clinic_Management.ClinicalConsultationService.controller;

import Clinic_Management.ClinicalConsultationService.dto.PrescriptionDetailResponse;
import Clinic_Management.ClinicalConsultationService.entity.Drug;
import Clinic_Management.ClinicalConsultationService.entity.PrescriptionStatus;
import Clinic_Management.ClinicalConsultationService.service.PharmacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/pharmacy", "/api/v1/clinical/pharmacy"})
@RequiredArgsConstructor
@Tag(name = "Pharmacy Management API", description = "Dành cho Dược sĩ: Hàng đợi đơn thuốc, trừ tồn kho và cấp phát")
public class PharmacyController {

    private final PharmacyService pharmacyService;

    @GetMapping("/prescriptions")
    @Operation(summary = "Lấy hàng đợi đơn thuốc (Mặc định: PENDING_DISPENSE chờ phát)")
    public ResponseEntity<List<PrescriptionDetailResponse>> getQueue(
            @RequestParam(defaultValue = "PENDING_DISPENSE") PrescriptionStatus status) {
        return ResponseEntity.ok(pharmacyService.getPrescriptionsByStatus(status));
    }

    @GetMapping("/prescriptions/{id}")
    @Operation(summary = "Xem chi tiết đơn thuốc, kiểm tra số lượng tồn kho và tiền đồng chi trả")
    public ResponseEntity<PrescriptionDetailResponse> getDetails(@PathVariable Long id) {
        return ResponseEntity.ok(pharmacyService.getPrescriptionDetails(id));
    }

    @PostMapping("/prescriptions/{id}/dispense")
    @Operation(summary = "Dược sĩ bấm xác nhận phát thuốc (Tự động trừ số lượng tồn kho)")
    public ResponseEntity<PrescriptionDetailResponse> dispense(
            @PathVariable Long id,
            @RequestParam Long pharmacistId,
            @RequestParam String pharmacistName) {
        return ResponseEntity.ok(pharmacyService.dispensePrescription(id, pharmacistId, pharmacistName));
    }

    @GetMapping("/drugs")
    @Operation(summary = "Lấy danh mục thuốc khả dụng trong kho phục vụ kê đơn")
    public ResponseEntity<List<Drug>> getDrugs() {
        return ResponseEntity.ok(pharmacyService.getActiveDrugs());
    }

    @PostMapping("/drugs")
    @Operation(summary = "Admin thêm loại thuốc mới vào kho dược")
    public ResponseEntity<Drug> createDrug(@Valid @RequestBody Clinic_Management.ClinicalConsultationService.dto.CreateDrugRequest request) {
        return ResponseEntity.ok(pharmacyService.createDrug(request));
    }

    @PutMapping("/drugs/{id}")
    @Operation(summary = "Admin cập nhật thông tin, đơn giá hoặc tỷ lệ BHYT của thuốc")
    public ResponseEntity<Drug> updateDrug(
            @PathVariable Long id,
            @Valid @RequestBody Clinic_Management.ClinicalConsultationService.dto.UpdateDrugRequest request) {
        return ResponseEntity.ok(pharmacyService.updateDrug(id, request));
    }

    @PatchMapping("/drugs/{id}/stock")
    @Operation(summary = "Admin nhập thêm hoặc điều chỉnh tồn kho thuốc")
    public ResponseEntity<Drug> adjustStock(
            @PathVariable Long id,
            @Valid @RequestBody Clinic_Management.ClinicalConsultationService.dto.AdjustStockRequest request) {
        return ResponseEntity.ok(pharmacyService.adjustDrugStock(id, request.getQuantityChange()));
    }

    @GetMapping("/drugs/available")
    @Operation(summary = "Lấy danh mục thuốc còn tồn kho (stock > 0), còn hạn và đang active phục vụ AI gợi ý đơn thuốc")
    public ResponseEntity<List<Drug>> getAvailableDrugs() {
        return ResponseEntity.ok(pharmacyService.getAvailableDrugsForPrescription());
    }
}