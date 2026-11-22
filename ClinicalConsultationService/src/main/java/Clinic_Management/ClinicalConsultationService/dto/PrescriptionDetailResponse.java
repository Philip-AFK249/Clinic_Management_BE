package Clinic_Management.ClinicalConsultationService.dto;

import Clinic_Management.ClinicalConsultationService.entity.PrescriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PrescriptionDetailResponse {
    private Long prescriptionId;
    private Long encounterId;
    private String ticketNumber;
    private Long patientId;
    private String patientName;
    private String insuranceCode;
    private Long doctorId;
    private String doctorName;
    private Long pharmacistId;
    private String pharmacistName;
    private PrescriptionStatus status;
    private BigDecimal totalAmount;
    private BigDecimal insurancePaidAmount;
    private BigDecimal patientCopayAmount;
    private LocalDateTime createdAt;
    private LocalDateTime dispensedAt;
    private List<PrescriptionItemResponse> items;

    @Data
    @Builder
    public static class PrescriptionItemResponse {
        private Long itemId;
        private Long drugId;
        private String drugCode;
        private String drugName;
        private String concentration;
        private String dosageForm;
        private String unit;
        private String routeFrequency;
        private String duration;
        private Integer quantity;
        private Integer currentStock;
        private BigDecimal unitPrice;
        private BigDecimal amount;
    }
}