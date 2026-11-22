package Clinic_Management.ClinicalConsultationService.service;

import Clinic_Management.ClinicalConsultationService.dto.BatchRestockInvoiceRequest;
import Clinic_Management.ClinicalConsultationService.dto.PrescriptionDetailResponse;
import Clinic_Management.ClinicalConsultationService.dto.StockInvoiceResponse;
import Clinic_Management.ClinicalConsultationService.entity.*;
import Clinic_Management.ClinicalConsultationService.repository.DrugRepository;
import Clinic_Management.ClinicalConsultationService.repository.PrescriptionRepository;
import Clinic_Management.ClinicalConsultationService.repository.StockInvoiceRepository;
import jakarta.annotation.PostConstruct;
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
    private final StockInvoiceRepository stockInvoiceRepository;

    @Transactional(readOnly = true)
    public List<PrescriptionDetailResponse> getPrescriptionsByStatus(PrescriptionStatus status) {
        List<Prescription> prescriptions = (status == PrescriptionStatus.DISPENSED || status == PrescriptionStatus.CANCELLED)
                ? prescriptionRepository.findHistoryByStatus(status)
                : prescriptionRepository.findQueueByStatus(status);
        return prescriptions.stream()
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

    /**
     * Bổ sung đơn vị tính cho các thuốc đã có trong kho nhưng chưa được khai báo,
     * dựa trên dạng bào chế (dosageForm) đã lưu.
     */
    @PostConstruct
    @Transactional
    public void backfillDrugUnits() {
        List<Drug> drugs = drugRepository.findAll();
        List<Drug> toUpdate = new java.util.ArrayList<>();
        for (Drug drug : drugs) {
            if (drug.getUnit() == null || drug.getUnit().isBlank()) {
                drug.setUnit(resolveUnitFromDosageForm(drug.getDosageForm()));
                toUpdate.add(drug);
            }
        }
        if (!toUpdate.isEmpty()) {
            drugRepository.saveAll(toUpdate);
            log.info("Đã bổ sung đơn vị tính cho {} thuốc còn thiếu.", toUpdate.size());
        }
    }

    private String resolveUnitFromDosageForm(String dosageForm) {
        String form = dosageForm == null ? "" : dosageForm.toLowerCase();
        if (form.contains("gói")) {
            return "Gói";
        }
        if (form.contains("ống")) {
            return "Ống";
        }
        if (form.contains("bình") || form.contains("xịt")) {
            return "Bình";
        }
        if (form.contains("kem") || form.contains("mỡ") || form.contains("gel")) {
            return "Tuýp";
        }
        if (form.contains("siro") || form.contains("dung dịch") || form.contains("nhũ tương")
                || form.contains("hỗn dịch") || form.contains("nhỏ")) {
            return "Lọ";
        }
        return "Viên";
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
                        .unit(i.getUnit() != null && !i.getUnit().isBlank() ? i.getUnit() : i.getDrug().getUnit())
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
                .unit(req.getUnit().trim())
                .stockQuantity(req.getStockQuantity())
                .unitPrice(req.getUnitPrice())
                .bhytCoverage(req.getBhytCoverage())
                .isPenicillinClass(Boolean.TRUE.equals(req.getIsPenicillinClass()))
                .expiryDate(req.getExpiryDate())
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
        drug.setUnit(req.getUnit().trim());
        drug.setUnitPrice(req.getUnitPrice());
        drug.setStockQuantity(req.getStockQuantity());
        drug.setBhytCoverage(req.getBhytCoverage());
        drug.setIsPenicillinClass(Boolean.TRUE.equals(req.getIsPenicillinClass()));
        drug.setExpiryDate(req.getExpiryDate());
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

    @Transactional
    public StockInvoiceResponse batchRestockFromInvoice(BatchRestockInvoiceRequest req) {
        StockInvoice invoice = StockInvoice.builder()
                .invoiceNumber(req.getInvoiceNumber().trim())
                .supplierName(req.getSupplierName().trim())
                .invoiceDate(req.getInvoiceDate())
                .totalAmount(req.getTotalAmount())
                .verifiedBy(req.getVerifiedBy().trim())
                .notes(req.getNotes())
                .build();

        for (BatchRestockInvoiceRequest.BatchRestockItemDto itemDto : req.getItems()) {
            Drug drug = drugRepository.findById(itemDto.getDrugId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thuốc có ID: " + itemDto.getDrugId()));

            drug.setStockQuantity(drug.getStockQuantity() + itemDto.getQuantityAdded());
            if (itemDto.getExpiryDate() != null) {
                drug.setExpiryDate(itemDto.getExpiryDate());
            }
            if (Boolean.TRUE.equals(itemDto.getUpdateCatalogPrice())
                    && itemDto.getImportUnitPrice() != null
                    && itemDto.getImportUnitPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
                drug.setUnitPrice(itemDto.getImportUnitPrice());
            }
            drugRepository.save(drug);

            invoice.addItem(StockInvoiceItem.builder()
                    .drug(drug)
                    .ocrDrugName(itemDto.getOcrDrugName())
                    .batchNumber(itemDto.getBatchNumber())
                    .expiryDate(itemDto.getExpiryDate())
                    .quantityAdded(itemDto.getQuantityAdded())
                    .importUnitPrice(itemDto.getImportUnitPrice())
                    .build());
        }

        StockInvoice saved = stockInvoiceRepository.save(invoice);
        log.info("Đã nhập kho theo hóa đơn {} (NCC: {}) bởi {}, gồm {} mặt hàng",
                saved.getInvoiceNumber(), saved.getSupplierName(), saved.getVerifiedBy(), saved.getItems().size());
        return mapToStockInvoiceResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<StockInvoiceResponse> getStockInvoices() {
        return stockInvoiceRepository.findAllWithItemsOrderByCreatedAtDesc().stream()
                .map(this::mapToStockInvoiceResponse)
                .toList();
    }

    private StockInvoiceResponse mapToStockInvoiceResponse(StockInvoice inv) {
        List<StockInvoiceResponse.StockInvoiceItemResponse> itemDtos = inv.getItems().stream()
                .map(i -> StockInvoiceResponse.StockInvoiceItemResponse.builder()
                        .itemId(i.getId())
                        .drugId(i.getDrug().getId())
                        .drugCode(i.getDrug().getCode())
                        .drugName(i.getDrug().getName())
                        .ocrDrugName(i.getOcrDrugName())
                        .batchNumber(i.getBatchNumber())
                        .expiryDate(i.getExpiryDate())
                        .quantityAdded(i.getQuantityAdded())
                        .newStockQuantity(i.getDrug().getStockQuantity())
                        .importUnitPrice(i.getImportUnitPrice())
                        .build())
                .toList();

        return StockInvoiceResponse.builder()
                .id(inv.getId())
                .invoiceNumber(inv.getInvoiceNumber())
                .supplierName(inv.getSupplierName())
                .invoiceDate(inv.getInvoiceDate())
                .totalAmount(inv.getTotalAmount())
                .verifiedBy(inv.getVerifiedBy())
                .notes(inv.getNotes())
                .createdAt(inv.getCreatedAt())
                .items(itemDtos)
                .build();
    }
}