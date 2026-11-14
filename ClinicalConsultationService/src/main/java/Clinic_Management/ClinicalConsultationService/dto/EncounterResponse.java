package Clinic_Management.ClinicalConsultationService.dto;

import Clinic_Management.ClinicalConsultationService.entity.EncounterStatus;
import Clinic_Management.ClinicalConsultationService.entity.PrescriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class EncounterResponse {
    private Long encounterId;
    private String ticketNumber;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private Long departmentId;
    private LocalDate encounterDate;
    private EncounterStatus status;
    private String subjective;
    private String objective;
    private String assessment;
    private String plan;
    private String bloodPressure;
    private Integer heartRate;
    private String temperature;
    private Integer spo2;
    private List<DiagnosisResponse> diagnoses;
    private Long prescriptionId;
    private PrescriptionStatus prescriptionStatus;
    private BigDecimal totalAmount;
    private BigDecimal insurancePaidAmount;
    private BigDecimal patientCopayAmount;
    private LocalDateTime dispensedAt;
    private List<PrescriptionDetailResponse.PrescriptionItemResponse> prescriptionItems;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class DiagnosisResponse {
        private String icd10Code;
        private String diseaseName;
        private Boolean isPrimary;
    }
}