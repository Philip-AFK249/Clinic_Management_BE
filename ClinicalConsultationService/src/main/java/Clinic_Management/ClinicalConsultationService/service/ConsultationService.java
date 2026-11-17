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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        Prescription savedPrescription = null;
        if (request.getPrescriptionItems() != null && !request.getPrescriptionItems().isEmpty()) {
            savedPrescription = createPrescription(savedEncounter, request.getPrescriptionItems(), request.getPatientAllergies());
        }

        intakeQueueClient.updateTicketStatus(savedEncounter.getTicketNumber(), "COMPLETED");

        log.info("Bác sĩ {} (ID={}) đã ký duyệt bệnh án #{}. Đơn thuốc ID: {}",
                encounter.getDoctorName(), encounter.getDoctorId(), encounter.getTicketNumber(),
                savedPrescription != null ? savedPrescription.getId() : null);

        return mapToEncounterResponse(savedEncounter, savedPrescription);
    }

    private Prescription createPrescription(Encounter encounter, List<PrescriptionItemRequest> itemRequests, List<String> allergies) {
        Prescription prescription = Prescription.builder()
                .encounterId(encounter.getId())
                .ticketNumber(encounter.getTicketNumber())
                .patientId(encounter.getPatientId())
                .patientName(encounter.getPatientName())
                .insuranceCode(encounter.getInsuranceCode())
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

            if (drug.getStockQuantity() <= 0) {
                throw new IllegalStateException(String.format(
                        "Thuốc %s (Mã %s) hiện đã HẾT HÀNG trong kho dược (Tồn: 0). Vui lòng chọn loại thuốc thay thế.",
                        drug.getName(), drug.getCode()
                ));
            }
            if (drug.getStockQuantity() < itemReq.getQuantity()) {
                throw new IllegalStateException(String.format(
                        "Thuốc %s (Mã %s) không đủ số lượng tồn kho. Hiện còn: %d, Bác sĩ đang kê: %d.",
                        drug.getName(), drug.getCode(), drug.getStockQuantity(), itemReq.getQuantity()
                ));
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

        return prescriptionRepository.save(prescription);
    }

    private EncounterResponse mapToEncounterResponse(Encounter e, Prescription prescription) {
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

        List<PrescriptionDetailResponse.PrescriptionItemResponse> rxItems = List.of();
        if (prescription != null && prescription.getItems() != null) {
            rxItems = prescription.getItems().stream()
                    .map(i -> PrescriptionDetailResponse.PrescriptionItemResponse.builder()
                            .itemId(i.getId())
                            .drugId(i.getDrug() != null ? i.getDrug().getId() : null)
                            .drugCode(i.getDrug() != null ? i.getDrug().getCode() : "")
                            .drugName(i.getDrugName())
                            .concentration(i.getDrug() != null ? i.getDrug().getConcentration() : "")
                            .dosageForm(i.getDosageForm())
                            .routeFrequency(i.getRouteFrequency())
                            .duration(i.getDuration())
                            .quantity(i.getQuantity())
                            .currentStock(i.getDrug() != null ? i.getDrug().getStockQuantity() : 0)
                            .unitPrice(i.getUnitPrice())
                            .amount(i.getAmount())
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
                .departmentId(e.getDepartmentId())
                .encounterDate(e.getEncounterDate())
                .status(e.getStatus())
                .subjective(e.getSubjective())
                .objective(e.getObjective())
                .assessment(e.getAssessment())
                .plan(e.getPlan())
                .bloodPressure(e.getBloodPressure())
                .heartRate(e.getHeartRate())
                .temperature(e.getTemperature())
                .spo2(e.getSpo2())
                .diagnoses(diagList)
                .prescriptionId(prescription != null ? prescription.getId() : null)
                .prescriptionStatus(prescription != null ? prescription.getStatus() : null)
                .totalAmount(prescription != null ? prescription.getTotalAmount() : BigDecimal.ZERO)
                .insurancePaidAmount(prescription != null ? prescription.getInsurancePaidAmount() : BigDecimal.ZERO)
                .patientCopayAmount(prescription != null ? prescription.getPatientCopayAmount() : BigDecimal.ZERO)
                .dispensedAt(prescription != null ? prescription.getDispensedAt() : null)
                .prescriptionItems(rxItems)
                .createdAt(e.getCreatedAt())
                .build();
    }

    private List<EncounterResponse> mapEncountersWithPrescriptions(List<Encounter> encounters) {
        if (encounters.isEmpty()) {
            return List.of();
        }
        List<Long> encounterIds = encounters.stream().map(Encounter::getId).toList();
        Map<Long, Prescription> rxByEncounterId = prescriptionRepository
                .findByEncounterIdInWithItems(encounterIds)
                .stream()
                .collect(Collectors.toMap(
                        Prescription::getEncounterId,
                        Function.identity(),
                        (existing, replacement) -> replacement
                ));

        return encounters.stream()
                .map(encounter -> mapToEncounterResponse(encounter, rxByEncounterId.get(encounter.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EncounterResponse> getPatientHistory(Long patientId) {
        return mapEncountersWithPrescriptions(encounterRepository.findPatientHistory(patientId));
    }

    @Transactional(readOnly = true)
    public List<EncounterResponse> getEncountersByTicketNumbers(List<String> ticketNumbers) {
        if (ticketNumbers == null || ticketNumbers.isEmpty()) {
            return List.of();
        }
        List<String> cleaned = ticketNumbers.stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
        if (cleaned.isEmpty()) {
            return List.of();
        }
        return mapEncountersWithPrescriptions(encounterRepository.findByTicketNumberIn(cleaned));
    }

    @Transactional(readOnly = true)
    public List<EncounterResponse> getDoctorEncounters(Long doctorId) {
        return mapEncountersWithPrescriptions(encounterRepository.findDoctorEncounters(doctorId));
    }

    @Transactional
    public EncounterResponse cancelEncounter(Long encounterId) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bệnh án có ID: " + encounterId));

        if (encounter.getStatus() == EncounterStatus.COMPLETED) {
            throw new IllegalStateException("Ca khám này đã hoàn tất, không thể hủy.");
        }

        if (encounter.getStatus() != EncounterStatus.CANCELLED) {
            encounter.setStatus(EncounterStatus.CANCELLED);
            encounter = encounterRepository.save(encounter);
            intakeQueueClient.updateTicketStatus(encounter.getTicketNumber(), "CANCELLED");
            log.info("Đã hủy ca khám ID={} (Phiếu: {})", encounter.getId(), encounter.getTicketNumber());
        }

        return mapToEncounterResponse(encounter, null);
    }

    @Transactional
    public EncounterResponse startEncounter(StartEncounterRequest request) {
        LocalDate today = LocalDate.now();

        // Tự động hủy các ca IN_PROGRESS cũ/bị treo của bệnh nhân này trước khi mở ca mới
        List<Encounter> staleEncounters = encounterRepository
                .findByPatientIdAndStatus(request.getPatientId(), EncounterStatus.IN_PROGRESS);
        for (Encounter stale : staleEncounters) {
            boolean isDifferentTicket = !stale.getTicketNumber().equals(request.getTicketNumber());
            boolean isPreviousDay = stale.getEncounterDate() != null && !stale.getEncounterDate().equals(today);
            if (isDifferentTicket || isPreviousDay) {
                stale.setStatus(EncounterStatus.CANCELLED);
                encounterRepository.save(stale);
                intakeQueueClient.updateTicketStatus(stale.getTicketNumber(), "CANCELLED");
                log.info("Tự động hủy ca khám bị treo ID={} (Phiếu: {}, Ngày: {}) cho BN ID={}",
                        stale.getId(), stale.getTicketNumber(), stale.getEncounterDate(), request.getPatientId());
            }
        }

        Encounter encounter = encounterRepository
                .findTopByTicketNumberAndEncounterDateOrderByCreatedAtDesc(request.getTicketNumber(), today)
                .orElseGet(() -> Encounter.builder()
                        .ticketNumber(request.getTicketNumber())
                        .patientId(request.getPatientId())
                        .patientName(request.getPatientName())
                        .insuranceCode(request.getInsuranceCode())
                        .doctorId(request.getDoctorId())
                        .doctorName(request.getDoctorName())
                        .departmentId(request.getDepartmentId())
                        .encounterDate(today)
                        .status(EncounterStatus.IN_PROGRESS)
                        .bloodPressure(request.getBloodPressure())
                        .heartRate(request.getHeartRate())
                        .temperature(request.getTemperature())
                        .spo2(request.getSpo2())
                        .build());

        encounter.setDoctorId(request.getDoctorId());
        encounter.setDoctorName(request.getDoctorName());
        encounter.setPatientId(request.getPatientId());
        encounter.setPatientName(request.getPatientName());
        if (request.getInsuranceCode() != null && !request.getInsuranceCode().isBlank()) {
            encounter.setInsuranceCode(request.getInsuranceCode());
        }
        encounter.setDepartmentId(request.getDepartmentId());

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
}