package Clinic_Management.ClinicalConsultationService.service;

import Clinic_Management.ClinicalConsultationService.dto.PrescriptionDetailResponse;
import Clinic_Management.ClinicalConsultationService.entity.Drug;
import Clinic_Management.ClinicalConsultationService.entity.Prescription;
import Clinic_Management.ClinicalConsultationService.entity.PrescriptionItem;
import Clinic_Management.ClinicalConsultationService.entity.PrescriptionStatus;
import Clinic_Management.ClinicalConsultationService.repository.DrugRepository;
import Clinic_Management.ClinicalConsultationService.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PharmacyService {

    private final PrescriptionRepository prescriptionRepository;
    private final DrugRepository drugRepository;


    @Transactional(readOnly = true)
    public List<PrescriptionDetailResponse> getPrescriptionsByStatus(PrescriptionStatus status) {
        return prescriptionRepository.findQueueByStatus(status).stream()
                .map(this::mapToDetailResponse)
                .toList();
    }


    @Transactional(readOnly = true)
    public PrescriptionDetailResponse getPrescriptionDetails(Long prescriptionId) {
        Prescription p = prescriptionRepository.findByIdWithItems(prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn thuốc ID: " + prescriptionId));
        return mapToDetailResponse(p);
    }

    @Transactional
    public PrescriptionDetailResponse dispensePrescription(Long prescriptionId, Long pharmacistId, String pharmacistName) {
        Prescription prescription = prescriptionRepository.findByIdWithItems(prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn thuốc ID: " + prescriptionId));

        if (prescription.getStatus() == PrescriptionStatus.DISPENSED) {
            throw new IllegalStateException("Đơn thuốc này đã được xuất kho và phát thành công trước đó.");
        }

        for (PrescriptionItem item : prescription.getItems()) {
            Drug drug = item.getDrug();
            if (drug.getStockQuantity() < item.getQuantity()) {
                throw new IllegalStateException(String.format(
                        "Thuốc %s (Mã %s) không đủ tồn kho để xuất. Tồn kho: %d, Cần xuất: %d",
                        drug.getName(), drug.getCode(), drug.getStockQuantity(), item.getQuantity()
                ));
            }
            drug.setStockQuantity(drug.getStockQuantity() - item.getQuantity());
            drugRepository.save(drug);
        }

        prescription.setStatus(PrescriptionStatus.DISPENSED);
        prescription.setPharmacistId(pharmacistId);
        prescription.setPharmacistName(pharmacistName);
        prescription.setDispensedAt(LocalDateTime.now());

        Prescription saved = prescriptionRepository.save(prescription);
        log.info("Dược sĩ {} đã phát thành công đơn thuốc #{}, thu viện phí: {} đ",
                pharmacistName, saved.getId(), saved.getPatientCopayAmount());

        return mapToDetailResponse(saved);
    }


    @Transactional(readOnly = true)
    public List<Drug> getActiveDrugs() {
        return drugRepository.findByActiveTrueOrderByNameAsc();
    }

    private PrescriptionDetailResponse mapToDetailResponse(Prescription p) {
        List<PrescriptionDetailResponse.PrescriptionItemResponse> itemResponses = p.getItems().stream()
                .map(i -> PrescriptionDetailResponse.PrescriptionItemResponse.builder()
                        .itemId(i.getId())
                        .drugId(i.getDrug().getId())
                        .drugCode(i.getDrug().getCode())
                        .drugName(i.getDrugName())
                        .concentration(i.getDrug().getConcentration())
                        .dosageForm(i.getDosageForm())
                        .routeFrequency(i.getRouteFrequency())
                        .duration(i.getDuration())
                        .quantity(i.getQuantity())
                        .currentStock(i.getDrug().getStockQuantity())
                        .unitPrice(i.getUnitPrice())
                        .amount(i.getAmount())
                        .build())
                .toList();

        return PrescriptionDetailResponse.builder()
                .prescriptionId(p.getId())
                .encounterId(p.getEncounterId())
                .ticketNumber(p.getTicketNumber())
                .patientId(p.getPatientId())
                .patientName(p.getPatientName())
                .insuranceCode(p.getInsuranceCode())
                .doctorId(p.getDoctorId())
                .doctorName(p.getDoctorName())
                .pharmacistId(p.getPharmacistId())
                .pharmacistName(p.getPharmacistName())
                .status(p.getStatus())
                .totalAmount(p.getTotalAmount())
                .insurancePaidAmount(p.getInsurancePaidAmount())
                .patientCopayAmount(p.getPatientCopayAmount())
                .createdAt(p.getCreatedAt())
                .dispensedAt(p.getDispensedAt())
                .items(itemResponses)
                .build();
    }

    @Transactional
    public Drug createDrug(Clinic_Management.ClinicalConsultationService.dto.CreateDrugRequest req) {
        if (drugRepository.findByCode(req.getCode().trim().toUpperCase()).isPresent()) {
            throw new IllegalStateException("Mã thuốc " + req.getCode() + " đã tồn tại trong kho.");
        }

        Drug drug = Drug.builder()
                .code(req.getCode().trim().toUpperCase())
                .name(req.getName().trim())
                .concentration(req.getConcentration().trim())
                .dosageForm(req.getDosageForm().trim())
                .stockQuantity(req.getStockQuantity())
                .unitPrice(req.getUnitPrice())
                .bhytCoverage(req.getBhytCoverage())
                .isPenicillinClass(Boolean.TRUE.equals(req.getIsPenicillinClass()))
                .active(true)
                .build();

        Drug saved = drugRepository.save(drug);
        log.info("Admin đã thêm mới thuốc vào kho: {} (Mã {})", saved.getName(), saved.getCode());
        return saved;
    }

    @Transactional
    public Drug updateDrug(Long id, Clinic_Management.ClinicalConsultationService.dto.UpdateDrugRequest req) {
        Drug drug = drugRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thuốc có ID: " + id));

        drug.setName(req.getName().trim());
        drug.setConcentration(req.getConcentration().trim());
        drug.setDosageForm(req.getDosageForm().trim());
        drug.setUnitPrice(req.getUnitPrice());
        drug.setStockQuantity(req.getStockQuantity());
        drug.setBhytCoverage(req.getBhytCoverage());
        drug.setIsPenicillinClass(Boolean.TRUE.equals(req.getIsPenicillinClass()));
        if (req.getActive() != null) {
            drug.setActive(req.getActive());
        }

        Drug updated = drugRepository.save(drug);
        log.info("Admin đã cập nhật thông tin thuốc #{}: {}", id, updated.getName());
        return updated;
    }

    @Transactional
    public Drug adjustDrugStock(Long id, Integer quantityChange) {
        Drug drug = drugRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thuốc có ID: " + id));

        int newStock = drug.getStockQuantity() + quantityChange;
        if (newStock < 0) {
            throw new IllegalStateException(String.format(
                    "Không thể giảm tồn kho xuống dưới 0. Tồn hiện tại: %d, lượng giảm: %d",
                    drug.getStockQuantity(), Math.abs(quantityChange)
            ));
        }

        drug.setStockQuantity(newStock);
        Drug updated = drugRepository.save(drug);
        log.info("Điều chỉnh tồn kho thuốc #{}: {} -> {}", id, drug.getStockQuantity() - quantityChange, newStock);
        return updated;
    }

    @Transactional(readOnly = true)
    public List<Drug> getAvailableDrugsForPrescription() {
        return drugRepository.findAvailableDrugsForPrescription(LocalDate.now());
    }
}