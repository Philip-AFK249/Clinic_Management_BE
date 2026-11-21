package Clinic_Management.PatientIntakeService.service;

import Clinic_Management.PatientIntakeService.clients.ScheduleServiceClient;
import Clinic_Management.PatientIntakeService.dto.QueueTicketDetailResponse;
import Clinic_Management.PatientIntakeService.entity.IntakeSource;
import Clinic_Management.PatientIntakeService.entity.Patient;
import Clinic_Management.PatientIntakeService.entity.QueueStatus;
import Clinic_Management.PatientIntakeService.entity.QueueTicket;
import Clinic_Management.PatientIntakeService.repository.QueueTicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class QueueService {

    private final QueueTicketRepository queueTicketRepository;
    private final ScheduleServiceClient scheduleClient;
    private final RestClient aiGatewayClient;

    public QueueService(
            QueueTicketRepository queueTicketRepository,
            ScheduleServiceClient scheduleClient,
            @Value("${services.ai-gateway.base-url:http://localhost:8008}") String aiGatewayBaseUrl) {
        this.queueTicketRepository = queueTicketRepository;
        this.scheduleClient = scheduleClient;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(3).toMillis());
        this.aiGatewayClient = RestClient.builder()
                .baseUrl(aiGatewayBaseUrl)
                .requestFactory(factory)
                .build();
    }

    @Transactional
    public QueueTicket updateConfirmedDiagnosis(
            String ticketNumber,
            String icd10Code,
            String diseaseName,
            String doctorName) {
        QueueTicket ticket = queueTicketRepository
                .findByAppointmentDateAndTicketNumber(LocalDate.now(), ticketNumber)
                .or(() -> queueTicketRepository.findTopByTicketNumberOrderByAppointmentDateDesc(ticketNumber))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu khám có mã: " + ticketNumber));

        ticket.setConfirmedIcd10Code(icd10Code);
        ticket.setConfirmedDiseaseName(diseaseName);
        QueueTicket saved = queueTicketRepository.save(ticket);

        if (Boolean.TRUE.equals(saved.getIsOutOfDataset())
                || (saved.getTriageFlagId() != null && !saved.getTriageFlagId().isBlank())) {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("confirmed_icd10_code", icd10Code != null ? icd10Code : "");
                body.put("confirmed_disease_name", diseaseName != null ? diseaseName : "");
                body.put("confirmed_by_doctor", doctorName != null ? doctorName : saved.getDoctorName());
                if (saved.getTriageFlagId() != null && !saved.getTriageFlagId().isBlank()) {
                    body.put("flag_id", saved.getTriageFlagId());
                }

                aiGatewayClient.patch()
                        .uri("/api/v1/admin/flagged-symptoms/by-ticket/{ticketNumber}/confirm", ticketNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();
                log.info("Đã đồng bộ chẩn đoán thực tế của phiếu {} sang AI Gateway", ticketNumber);
            } catch (Exception ex) {
                log.warn("Không thể đồng bộ chẩn đoán thực tế của phiếu {} sang AI Gateway: {}",
                        ticketNumber, ex.getMessage());
            }
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<QueueTicket> getDoctorActiveQueue(Long doctorId, LocalDate date) {
        return queueTicketRepository.findDoctorActiveQueue(
                doctorId, date, List.of(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.IN_CONSULTATION)
        );
    }

    @Transactional
    public QueueTicket callNextPatient(Long doctorId, LocalDate date) {
        List<QueueTicket> activeConsultations = queueTicketRepository.findDoctorActiveQueue(
                doctorId, date, List.of(QueueStatus.CALLED, QueueStatus.IN_CONSULTATION)
        );

        if (!activeConsultations.isEmpty()) {
            QueueTicket ongoingTicket = activeConsultations.get(0);
            throw new IllegalStateException(String.format(
                    "Bác sĩ đang có ca khám chưa hoàn tất (Phiếu %s - %s). Vui lòng hoàn thành hoặc chuyển trạng thái ca hiện tại trước khi gọi số tiếp theo.",
                    ongoingTicket.getTicketNumber(),
                    ongoingTicket.getStatus() == QueueStatus.CALLED ? "Đang gọi vào phòng" : "Đang khám"
            ));
        }

        List<QueueTicket> waitingList = queueTicketRepository.findDoctorActiveQueue(
                doctorId, date, List.of(QueueStatus.WAITING)
        );

        if (waitingList.isEmpty()) {
            throw new IllegalStateException("Hàng đợi không còn bệnh nhân nào đang chờ.");
        }

        QueueTicket nextTicket = waitingList.get(0);
        nextTicket.setStatus(QueueStatus.CALLED);
        nextTicket.setCalledAt(LocalDateTime.now());

        log.info("Bác sĩ ID {} đã gọi bệnh nhân vào phòng. Phiếu: {}", doctorId, nextTicket.getTicketNumber());
        return queueTicketRepository.save(nextTicket);
    }

    @Transactional
    public QueueTicket updateTicketStatus(String ticketNumber, QueueStatus status) {
        QueueTicket ticket = queueTicketRepository
                .findByAppointmentDateAndTicketNumber(LocalDate.now(), ticketNumber)
                .or(() -> queueTicketRepository.findTopByTicketNumberOrderByAppointmentDateDesc(ticketNumber))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu khám có mã: " + ticketNumber));

        ticket.setStatus(status);
        QueueTicket saved = queueTicketRepository.save(ticket);
        log.info("Cập nhật trạng thái phiếu {} sang {}", ticketNumber, status);

        if (status == QueueStatus.CANCELLED
                && ticket.getIntakeSource() == Clinic_Management.PatientIntakeService.entity.IntakeSource.ONLINE_BOOKING
                && (ticket.getStatus() == QueueStatus.WAITING
                || ticket.getStatus() == QueueStatus.CALLED
                || ticket.getStatus() == QueueStatus.IN_CONSULTATION)) {
            throw new IllegalStateException(
                    "Vé đặt lịch trực tuyến đã được xác nhận chính thức lên hệ thống. Theo quy định phòng khám, không cho phép hủy lịch và không hoàn phí."
            );
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public QueueTicketDetailResponse getTicketDetails(String ticketNumber) {
        List<QueueTicket> tickets = queueTicketRepository.findByTicketNumberWithPatientOrderByDateDesc(ticketNumber);
        if (tickets.isEmpty()) {
            return null;
        }
        QueueTicket ticket = tickets.get(0);
        return mapToDetailResponse(ticket, ticket.getPatient());
    }

    @Transactional(readOnly = true)
    public QueueTicketDetailResponse getCurrentDoctorConsultation(Long doctorId) {
        List<QueueTicket> active = queueTicketRepository.findCurrentDoctorConsultation(
                doctorId, LocalDate.now(), List.of(QueueStatus.CALLED, QueueStatus.IN_CONSULTATION)
        );
        if (active.isEmpty()) {
            return null;
        }
        QueueTicket ticket = active.get(0);
        return mapToDetailResponse(ticket, ticket.getPatient());
    }

    @Transactional(readOnly = true)
    public List<QueueTicket> getReceptionHistoryQueue(Long departmentId, LocalDate fromDate) {
        LocalDate effectiveFrom = (fromDate != null) ? fromDate : LocalDate.now().minusDays(7);
        Long normalizedDeptId = (departmentId != null && departmentId > 0) ? departmentId : null;
        return queueTicketRepository.findReceptionHistoryQueue(
                normalizedDeptId,
                effectiveFrom,
                List.of(QueueStatus.COMPLETED)
        );
    }

    @Transactional(readOnly = true)
    public List<QueueTicket> getReceptionQueue(Long departmentId, LocalDate date) {
        if (departmentId != null && departmentId > 0) {
            return queueTicketRepository.findByDepartmentIdAndAppointmentDateAndStatusIn(
                    departmentId, date, List.of(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.IN_CONSULTATION)
            );
        }
        return queueTicketRepository.findByAppointmentDateAndStatusIn(
                date, List.of(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.IN_CONSULTATION)
        );
    }



    @Transactional
    public QueueTicketDetailResponse getActiveTicketByPatientId(Long patientId) {
        List<QueueTicket> tickets = queueTicketRepository.findActiveTicketsByPatientId(
                patientId,
                List.of(
                        QueueStatus.PENDING_CONFIRMATION,
                        QueueStatus.WAITING,
                        QueueStatus.CALLED,
                        QueueStatus.IN_CONSULTATION
                )
        );

        if (tickets.isEmpty()) {
            return null;
        }

        QueueTicket ticket = tickets.get(0);

        // Nếu vé online tạm thời đã quá hạn 3 phút -> tự động hủy và nhả slot
        if (ticket.getStatus() == QueueStatus.PENDING_CONFIRMATION
                && ticket.getExpiresAt() != null
                && !ticket.getExpiresAt().isAfter(LocalDateTime.now())) {
            ticket.setStatus(QueueStatus.CANCELLED);
            ticket.setExpiresAt(null);
            ticket.setRejectionReason("Quá thời hạn 3 phút chưa được xác thực, hệ thống tự động hủy.");
            queueTicketRepository.save(ticket);
            scheduleClient.releaseSlot(ticket.getTicketNumber());
            log.info("Vé tạm {} của BN ID {} đã quá hạn 3 phút, tự động chuyển sang CANCELLED",
                    ticket.getTicketNumber(), patientId);
            return null;
        }

        if (ticket.getAppointmentDate().isBefore(LocalDate.now())) {
            ticket.setStatus(QueueStatus.CANCELLED);
            ticket.setExpiresAt(null);
            queueTicketRepository.save(ticket);
            scheduleClient.releaseSlot(ticket.getTicketNumber());
            log.info("Phiếu {} ngày {} đã quá hạn so với hôm nay, tự động chuyển sang CANCELLED",
                    ticket.getTicketNumber(), ticket.getAppointmentDate());
            return null;
        }

        return mapToDetailResponse(ticket, ticket.getPatient());
    }

    @Transactional
    public QueueTicketDetailResponse getActiveTicketByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }

        List<QueueTicket> tickets = queueTicketRepository.findActiveTicketsByIdentifier(
                identifier.trim(),
                List.of(
                        QueueStatus.PENDING_CONFIRMATION,
                        QueueStatus.WAITING,
                        QueueStatus.CALLED,
                        QueueStatus.IN_CONSULTATION
                )
        );

        if (tickets.isEmpty()) {
            return null;
        }

        QueueTicket ticket = tickets.get(0);

        // Nếu vé online tạm thời đã quá hạn 3 phút -> tự động hủy và nhả slot
        if (ticket.getStatus() == QueueStatus.PENDING_CONFIRMATION
                && ticket.getExpiresAt() != null
                && !ticket.getExpiresAt().isAfter(LocalDateTime.now())) {
            ticket.setStatus(QueueStatus.CANCELLED);
            ticket.setExpiresAt(null);
            ticket.setRejectionReason("Quá thời hạn 3 phút chưa được xác thực, hệ thống tự động hủy.");
            queueTicketRepository.save(ticket);
            scheduleClient.releaseSlot(ticket.getTicketNumber());
            log.info("Vé tạm {} (định danh {}) đã quá hạn 3 phút, tự động chuyển sang CANCELLED",
                    ticket.getTicketNumber(), identifier);
            return null;
        }

        if (ticket.getAppointmentDate().isBefore(LocalDate.now())) {
            ticket.setStatus(QueueStatus.CANCELLED);
            ticket.setExpiresAt(null);
            queueTicketRepository.save(ticket);
            scheduleClient.releaseSlot(ticket.getTicketNumber());
            log.info("Phiếu {} ngày {} đã quá hạn so với hôm nay, tự động chuyển sang CANCELLED",
                    ticket.getTicketNumber(), ticket.getAppointmentDate());
            return null;
        }

        return mapToDetailResponse(ticket, ticket.getPatient());
    }

    @Transactional(readOnly = true)
    public List<QueueTicketDetailResponse> getPatientBookingHistory(Long patientId, String identifier) {
        Map<String, QueueTicket> merged = new LinkedHashMap<>();

        if (patientId != null && patientId > 0) {
            for (QueueTicket t : queueTicketRepository.findBookingHistoryByPatientId(patientId)) {
                merged.put(t.getAppointmentDate() + "_" + t.getTicketNumber(), t);
            }
        }
        if (identifier != null && !identifier.isBlank()) {
            for (QueueTicket t : queueTicketRepository.findBookingHistoryByIdentifier(identifier.trim())) {
                merged.put(t.getAppointmentDate() + "_" + t.getTicketNumber(), t);
            }
        }

        return merged.values().stream()
                .sorted(Comparator
                        .comparing(QueueTicket::getAppointmentDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(QueueTicket::getSlotStartTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(t -> mapToDetailResponse(t, t.getPatient()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QueueTicket> getDoctorUpcomingQueue(Long doctorId, LocalDate fromDate) {
        return queueTicketRepository.findDoctorUpcomingQueue(
                doctorId, fromDate, List.of(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.IN_CONSULTATION)
        );
    }

    @Transactional
    public QueueTicket updateTicketVitals(String ticketNumber, Clinic_Management.PatientIntakeService.dto.UpdateVitalsRequest req) {
        QueueTicket ticket = queueTicketRepository
                .findByAppointmentDateAndTicketNumber(LocalDate.now(), ticketNumber)
                .or(() -> queueTicketRepository.findTopByTicketNumberOrderByAppointmentDateDesc(ticketNumber))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu khám: " + ticketNumber));

        ticket.setBloodPressure(req.getBloodPressure());
        ticket.setHeartRate(req.getHeartRate());
        ticket.setTemperature(req.getTemperature());
        ticket.setSpo2(req.getSpo2());
        ticket.setWeightKg(req.getWeightKg());
        ticket.setHeightCm(req.getHeightCm());
        ticket.setNurseNotes(req.getNurseNotes());

        log.info("Điều dưỡng cập nhật sinh hiệu cho phiếu {}: HA={}, Mạch={}, SpO2={}",
                ticketNumber, req.getBloodPressure(), req.getHeartRate(), req.getSpo2());

        return queueTicketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public List<QueueTicket> getReceptionUpcomingQueue(Long departmentId, LocalDate fromDate) {
        Long normalizedDeptId = (departmentId != null && departmentId > 0) ? departmentId : null;
        return queueTicketRepository.findReceptionUpcomingQueue(
                normalizedDeptId,
                fromDate,
                List.of(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.IN_CONSULTATION)
        );
    }

    @Transactional
    public List<QueueTicketDetailResponse> getPendingOnlineTickets() {
        expireOverdueTemporaryTickets();
        return queueTicketRepository
                .findByStatusWithPatientOrderByCheckInTimeAsc(QueueStatus.PENDING_CONFIRMATION)
                .stream()
                .map(t -> mapToDetailResponse(t, t.getPatient()))
                .toList();
    }

    @Transactional
    public QueueTicketDetailResponse approveOnlineTicket(String ticketNumber, String adminName) {
        QueueTicket ticket = queueTicketRepository
                .findByAppointmentDateAndTicketNumber(LocalDate.now(), ticketNumber)
                .or(() -> queueTicketRepository.findTopByTicketNumberOrderByAppointmentDateDesc(ticketNumber))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vé khám có mã: " + ticketNumber));

        if (ticket.getStatus() != QueueStatus.PENDING_CONFIRMATION) {
            throw new IllegalStateException("Chỉ có thể xác thực các vé online đang ở trạng thái tạm thời (PENDING_CONFIRMATION).");
        }

        LocalDateTime now = LocalDateTime.now();
        if (ticket.getExpiresAt() != null && !ticket.getExpiresAt().isAfter(now)) {
            ticket.setStatus(QueueStatus.CANCELLED);
            ticket.setExpiresAt(null);
            ticket.setRejectionReason("Quá thời hạn 3 phút chưa được xác thực, hệ thống đã tự động hủy.");
            queueTicketRepository.save(ticket);
            scheduleClient.releaseSlot(ticket.getTicketNumber());
            throw new IllegalStateException("Vé tạm thời này đã quá hạn 3 phút và đã bị hệ thống tự động hủy.");
        }

        // Xác nhận chính thức suất khám bên DoctorScheduleService (TEMPORARY -> BOOKED)
        scheduleClient.confirmSlot(ticket.getTicketNumber());

        ticket.setStatus(QueueStatus.WAITING);
        ticket.setExpiresAt(null);
        ticket.setConfirmedAt(now);
        ticket.setConfirmedBy((adminName != null && !adminName.isBlank()) ? adminName.trim() : "Quản trị viên");
        ticket.setIsReceiptVerified(true);

        QueueTicket saved = queueTicketRepository.save(ticket);
        log.info("Admin {} đã xác thực vé online {} thành VÉ CHÍNH THỨC.", saved.getConfirmedBy(), ticketNumber);
        return mapToDetailResponse(saved, saved.getPatient());
    }

    @Transactional
    public QueueTicketDetailResponse rejectOnlineTicket(String ticketNumber, String reason, String adminName) {
        QueueTicket ticket = queueTicketRepository
                .findByAppointmentDateAndTicketNumber(LocalDate.now(), ticketNumber)
                .or(() -> queueTicketRepository.findTopByTicketNumberOrderByAppointmentDateDesc(ticketNumber))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vé khám có mã: " + ticketNumber));

        if (ticket.getStatus() != QueueStatus.PENDING_CONFIRMATION) {
            throw new IllegalStateException("Vé này không còn ở trạng thái chờ xác thực.");
        }

        ticket.setStatus(QueueStatus.REJECTED);
        ticket.setExpiresAt(null);
        ticket.setConfirmedBy((adminName != null && !adminName.isBlank()) ? adminName.trim() : "Quản trị viên");
        ticket.setRejectionReason((reason != null && !reason.isBlank())
                ? reason.trim()
                : "Quản trị viên từ chối xác thực vé online.");

        QueueTicket saved = queueTicketRepository.save(ticket);
        scheduleClient.releaseSlot(ticket.getTicketNumber());

        log.info("Admin {} đã TỪ CHỐI vé online {} và giải phóng suất khám.", saved.getConfirmedBy(), ticketNumber);
        return mapToDetailResponse(saved, saved.getPatient());
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 10000)
    @Transactional
    public void expireOverdueTemporaryTickets() {
        List<QueueTicket> expiredTickets = queueTicketRepository.findExpiredTemporaryTickets(
                QueueStatus.PENDING_CONFIRMATION,
                LocalDateTime.now()
        );
        if (expiredTickets.isEmpty()) return;

        for (QueueTicket ticket : expiredTickets) {
            ticket.setStatus(QueueStatus.CANCELLED);
            ticket.setExpiresAt(null);
            ticket.setRejectionReason("Quá thời hạn 3 phút chưa được thanh toán/xác thực, hệ thống tự động hủy để nhường suất khám.");
            queueTicketRepository.save(ticket);
            scheduleClient.releaseSlot(ticket.getTicketNumber());
            log.info("Tự động hủy vé tạm quá hạn 3 phút: {} (BN: {})",
                    ticket.getTicketNumber(), ticket.getPatient().getFullName());
        }
    }

    private QueueTicketDetailResponse mapToDetailResponse(QueueTicket ticket, Patient patient) {
        return QueueTicketDetailResponse.builder()
                .ticketNumber(ticket.getTicketNumber())
                .departmentId(ticket.getDepartmentId())
                .departmentName(ticket.getDepartmentName())
                .doctorId(ticket.getDoctorId())
                .doctorName(ticket.getDoctorName())
                .roomNumber(ticket.getRoomNumber())
                .appointmentDate(ticket.getAppointmentDate())
                .slotStartTime(ticket.getSlotStartTime())
                .priorityLevel(ticket.getPriorityLevel())
                .intakeSource(ticket.getIntakeSource())
                .status(ticket.getStatus())
                .chiefComplaint(ticket.getChiefComplaint())
                .checkInTime(ticket.getCheckInTime())
                .calledAt(ticket.getCalledAt())
                .patientId(patient != null ? patient.getId() : null)
                .patientFullName(patient != null ? patient.getFullName() : null)
                .identityCardNumber(patient != null ? patient.getIdentityCardNumber() : null)
                .insuranceCode(patient != null ? patient.getInsuranceCode() : null)
                .initialHospitalCode(patient != null ? patient.getInitialHospitalCode() : null)
                .dateOfBirth(patient != null ? patient.getDateOfBirth() : null)
                .gender(patient != null ? patient.getGender() : null)
                .phone(patient != null ? patient.getPhone() : null)
                .address(patient != null ? patient.getAddress() : null)
                .isOcrVerified(patient != null && Boolean.TRUE.equals(patient.getIsOcrVerified()))
                .bloodPressure(ticket.getBloodPressure())
                .heartRate(ticket.getHeartRate())
                .temperature(ticket.getTemperature())
                .spo2(ticket.getSpo2())
                .weightKg(ticket.getWeightKg())
                .heightCm(ticket.getHeightCm())
                .nurseNotes(ticket.getNurseNotes())
                .secondaryDepartmentId(ticket.getSecondaryDepartmentId())
                .secondaryDepartmentName(ticket.getSecondaryDepartmentName())
                .multiSpecialtyNote(ticket.getMultiSpecialtyNote())
                .receiptNumber(ticket.getReceiptNumber())
                .receiptAmount(ticket.getReceiptAmount())
                .isReceiptVerified(Boolean.TRUE.equals(ticket.getIsReceiptVerified()))
                .triageFlagId(ticket.getTriageFlagId())
                .isOutOfDataset(Boolean.TRUE.equals(ticket.getIsOutOfDataset()))
                .flagReason(ticket.getFlagReason())
                .confirmedIcd10Code(ticket.getConfirmedIcd10Code())
                .confirmedDiseaseName(ticket.getConfirmedDiseaseName())
                .expiresAt(ticket.getExpiresAt())
                .confirmedAt(ticket.getConfirmedAt())
                .confirmedBy(ticket.getConfirmedBy())
                .rejectionReason(ticket.getRejectionReason())
                .canCancel(ticket.getIntakeSource() != IntakeSource.ONLINE_BOOKING
                        && ticket.getStatus() == QueueStatus.WAITING)
                .build();
    }
}