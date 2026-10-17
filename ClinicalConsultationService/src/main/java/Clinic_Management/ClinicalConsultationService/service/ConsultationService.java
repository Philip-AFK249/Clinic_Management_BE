package Clinic_Management.ClinicalConsultationService.service;

import Clinic_Management.ClinicalConsultationService.dto.*;
import Clinic_Management.ClinicalConsultationService.entity.*;
import Clinic_Management.ClinicalConsultationService.repository.DrugRepository;
import Clinic_Management.ClinicalConsultationService.repository.EncounterRepository;
import Clinic_Management.ClinicalConsultationService.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultationService {

    private final EncounterRepository encounterRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final DrugRepository drugRepository;
    private final IntakeQueueClient intakeQueueClient;

    @Transactional
    public EncounterResponse completeEncounter(Long encounterId, CompleteEncounterRequest request) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bệnh án có ID: " + encounterId));

        if (encounter.getStatus() == EncounterStatus.COMPLETED) {
            throw new IllegalStateException("Ca khám này đã được phê duyệt hoàn tất trước đó.");
        }

        encounter.setSubjective(request.getSubjective());
        encounter.setObjective(request.getObjective());
        encounter.setAssessment(request.getAssessment());
        encounter.setPlan(request.getPlan());
        encounter.setBloodPressure(request.getBloodPressure());
        encounter.setHeartRate(request.getHeartRate());
        encounter.setTemperature(request.getTemperature());
        encounter.setSpo2(request.getSpo2());
        encounter.setStatus(EncounterStatus.COMPLETED);

        encounter.getDiagnoses().clear();
        for (DiagnosisRequest diagReq : request.getDiagnoses()) {
            encounter.addDiagnosis(EncounterDiagnosis.builder()
                    .icd10Code(diagReq.getIcd10Code())
                    .diseaseName(diagReq.getDiseaseName())
                    .isPrimary(Boolean.TRUE.equals(diagReq.getIsPrimary()))
                    .aiConfidence(diagReq.getAiConfidence())
                    .build());
        }

        Encounter savedEncounter = encounterRepository.save(encounter);

        Long prescriptionId = null;
        if (request.getPrescriptionItems() != null && !request.getPrescriptionItems().isEmpty()) {
            prescriptionId = createPrescription(savedEncounter, request.getPrescriptionItems(), request.getPatientAllergies());
        }

        intakeQueueClient.updateTicketStatus(savedEncounter.getTicketNumber(), "COMPLETED");

        log.info("Bác sĩ {} đã ký duyệt bệnh án #{}. Đơn thuốc ID: {}",
                encounter.getDoctorName(), encounter.getTicketNumber(), prescriptionId);

        return mapToEncounterResponse(savedEncounter, prescriptionId);
    }

    private Long createPrescription(Encounter encounter, List<PrescriptionItemRequest> itemRequests, List<String> allergies) {
        Prescription prescription = Prescription.builder()
                .encounterId(encounter.getId())
                .ticketNumber(encounter.getTicketNumber())
                .patientId(encounter.getPatientId())
                .patientName(encounter.getPatientName())
                .doctorId(encounter.getDoctorId())
                .doctorName(encounter.getDoctorName())
                .status(PrescriptionStatus.PENDING_DISPENSE)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal insurancePaid = BigDecimal.ZERO;

        for (PrescriptionItemRequest itemReq : itemRequests) {
            Drug drug = drugRepository.findById(itemReq.getDrugId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thuốc có ID: " + itemReq.getDrugId()));

            if (drug.getIsPenicillinClass() && allergies != null) {
                boolean hasPenicillinAllergy = allergies.stream()
                        .anyMatch(a -> a.toLowerCase().contains("penicillin"));
                if (hasPenicillinAllergy) {
                    throw new IllegalStateException("CẢNH BÁO AN TOÀN: Bệnh nhân có tiền sử dị ứng với Penicillin! Không thể kê thuốc " + drug.getName());
                }
            }

            BigDecimal itemTotal = drug.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            if (drug.getBhytCoverage() == BhytCoverage.BHYT_80) {
                insurancePaid = insurancePaid.add(itemTotal.multiply(new BigDecimal("0.80")));
            } else if (drug.getBhytCoverage() == BhytCoverage.BHYT_100) {
                insurancePaid = insurancePaid.add(itemTotal);
            }

            PrescriptionItem item = PrescriptionItem.builder()
                    .drug(drug)
                    .drugName(drug.getName())
                    .dosageForm(drug.getDosageForm())
                    .routeFrequency(itemReq.getRouteFrequency())
                    .duration(itemReq.getDuration())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(drug.getUnitPrice())
                    .amount(itemTotal)
                    .build();

            prescription.addItem(item);
        }

        insurancePaid = insurancePaid.setScale(2, RoundingMode.HALF_UP);
        BigDecimal patientCopay = totalAmount.subtract(insurancePaid);

        prescription.setTotalAmount(totalAmount);
        prescription.setInsurancePaidAmount(insurancePaid);
        prescription.setPatientCopayAmount(patientCopay);

        Prescription saved = prescriptionRepository.save(prescription);
        return saved.getId();
    }

    @Transactional
    public EncounterResponse startEncounter(StartEncounterRequest request) {
        Encounter encounter = encounterRepository.findByTicketNumber(request.getTicketNumber())
                .orElseGet(() -> Encounter.builder()
                        .ticketNumber(request.getTicketNumber())
                        .patientId(request.getPatientId())
                        .patientName(request.getPatientName())
                        .doctorId(request.getDoctorId())
                        .doctorName(request.getDoctorName())
                        .departmentId(request.getDepartmentId())
                        .encounterDate(LocalDate.now())
                        .status(EncounterStatus.IN_PROGRESS)
                        .bloodPressure(request.getBloodPressure())
                        .heartRate(request.getHeartRate())
                        .temperature(request.getTemperature())
                        .spo2(request.getSpo2())
                        .build());

        if (encounter.getBloodPressure() == null && request.getBloodPressure() != null) {
            encounter.setBloodPressure(request.getBloodPressure());
            encounter.setHeartRate(request.getHeartRate());
            encounter.setTemperature(request.getTemperature());
            encounter.setSpo2(request.getSpo2());
        }

        Encounter saved = encounterRepository.save(encounter);
        intakeQueueClient.updateTicketStatus(request.getTicketNumber(), "IN_CONSULTATION");

        return mapToEncounterResponse(saved, null);
    }

    private EncounterResponse mapToEncounterResponse(Encounter e, Long prescriptionId) {
        List<EncounterResponse.DiagnosisResponse> diagList = new ArrayList<>();
        if (e.getDiagnoses() != null) {
            diagList = e.getDiagnoses().stream()
                    .map(d -> EncounterResponse.DiagnosisResponse.builder()
                            .icd10Code(d.getIcd10Code())
                            .diseaseName(d.getDiseaseName())
                            .isPrimary(d.getIsPrimary())
                            .build())
                    .toList();
        }

        return EncounterResponse.builder()
                .encounterId(e.getId())
                .ticketNumber(e.getTicketNumber())
                .patientId(e.getPatientId())
                .patientName(e.getPatientName())
                .doctorId(e.getDoctorId())
                .doctorName(e.getDoctorName())
                .encounterDate(e.getEncounterDate())
                .status(e.getStatus())
                .subjective(e.getSubjective())
                .objective(e.getObjective())
                .assessment(e.getAssessment())
                .plan(e.getPlan())
                .diagnoses(diagList)
                .prescriptionId(prescriptionId)
                .createdAt(e.getCreatedAt())
                .build();
    }
    @Transactional(readOnly = true)
    public List<EncounterResponse> getPatientHistory(Long patientId) {
        return encounterRepository.findPatientHistory(patientId).stream()
                .map(encounter -> mapToEncounterResponse(encounter, null))
                .toList();
    }
}