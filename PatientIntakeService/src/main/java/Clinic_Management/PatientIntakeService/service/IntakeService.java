package Clinic_Management.PatientIntakeService.service;

import Clinic_Management.PatientIntakeService.clients.ScheduleServiceClient;
import Clinic_Management.PatientIntakeService.dto.*;
import Clinic_Management.PatientIntakeService.entity.*;
import Clinic_Management.PatientIntakeService.repository.PatientRepository;
import Clinic_Management.PatientIntakeService.repository.QueueTicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class IntakeService {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MAX_TICKET_GEN_RETRIES = 3;

    private final PatientRepository patientRepository;
    private final QueueTicketRepository queueTicketRepository;
    private final ScheduleServiceClient scheduleClient;
    private final RestClient aiGatewayClient;

    public IntakeService(
            PatientRepository patientRepository,
            QueueTicketRepository queueTicketRepository,
            ScheduleServiceClient scheduleClient,
            @Value("${services.ai-gateway.base-url:http://localhost:8008}") String aiGatewayBaseUrl) {
        this.patientRepository = patientRepository;
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

    private Patient resolvePatient(AdministrativeIntakeRequest req) {
        if (req.getIdentityCardNumber() != null && !req.getIdentityCardNumber().isBlank()) {
            return patientRepository.findByIdentityCardNumber(req.getIdentityCardNumber())
                    .map(existing -> updatePatientDetails(existing, req))
                    .orElseGet(() -> createPatient(req));
        }
        if (req.getInsuranceCode() != null && !req.getInsuranceCode().isBlank()) {
            return patientRepository.findByInsuranceCode(req.getInsuranceCode())
                    .map(existing -> updatePatientDetails(existing, req))
                    .orElseGet(() -> createPatient(req));
        }
        return createPatient(req);
    }

    private Patient createPatient(AdministrativeIntakeRequest req) {
        Patient patient = Patient.builder()
                .fullName(req.getFullName().trim())
                .identityCardNumber(req.getIdentityCardNumber())
                .insuranceCode(req.getInsuranceCode())
                .initialHospitalCode(req.getInitialHospitalCode())
                .dateOfBirth(req.getDateOfBirth())
                .gender(req.getGender())
                .phone(req.getPhone())
                .address(req.getAddress())
                .isOcrVerified(Boolean.TRUE.equals(req.getIsOcrVerified()))
                .build();
        return patientRepository.save(patient);
    }

    private Patient updatePatientDetails(Patient patient, AdministrativeIntakeRequest req) {
        patient.setFullName(req.getFullName().trim());
        if (req.getInsuranceCode() != null) {
            String cleanedBhyt = cleanInsuranceCode(req.getInsuranceCode());
            patient.setInsuranceCode(cleanedBhyt);
        }
        if (req.getPhone() != null && !req.getPhone().isBlank()) patient.setPhone(req.getPhone().trim());
        if (req.getAddress() != null && !req.getAddress().isBlank()) patient.setAddress(req.getAddress().trim());
        if (req.getInitialHospitalCode() != null && !req.getInitialHospitalCode().isBlank()) {
            patient.setInitialHospitalCode(req.getInitialHospitalCode().trim());
        }
        if (req.getDateOfBirth() != null) patient.setDateOfBirth(req.getDateOfBirth());
        if (req.getGender() != null && !req.getGender().isBlank()) patient.setGender(req.getGender().trim());
        if (req.getIdentityCardNumber() != null && !req.getIdentityCardNumber().isBlank()) {
            patient.setIdentityCardNumber(req.getIdentityCardNumber().trim());
        }

        if (patient.getInsuranceCode() == null || patient.getInsuranceCode().isBlank()) {
            patient.setIsOcrVerified(false);
        } else if (Boolean.TRUE.equals(req.getIsOcrVerified())) {
            patient.setIsOcrVerified(true);
        }
        return patientRepository.save(patient);
    }

    private String generateTicketNumber(Long departmentId, LocalDate date, int offset) {
        long currentCount = queueTicketRepository.countByAppointmentDateAndDepartmentId(date, departmentId) + 1 + offset;
        String prefix = switch (departmentId.intValue()) {
            case 1 -> "A"; // Khoa Nội Tổng quát & Tim mạch
            case 2 -> "B"; // Khoa Hô hấp & Dị ứng
            case 3 -> "C"; // Khoa Da liễu
            default -> "K";
        };
        return String.format("#%s-%03d", prefix, currentCount);
    }

    private String cleanInsuranceCode(String code) {
        if (code == null) return null;
        String cleaned = code.replaceAll("\\s+", "").toUpperCase().trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private void linkFlagToTicket(String flagId, String ticketNumber, String patientName,
                                  String flagReason, boolean isOutOfDataset) {
        if (flagId == null || flagId.isBlank()) return;
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("ticket_number", ticketNumber);
            body.put("patient_name", patientName != null ? patientName : "");
            body.put("flag_reason", (flagReason == null || flagReason.isBlank())
                    ? (isOutOfDataset ? "NEW_SYMPTOM_PATTERN" : "")
                    : flagReason.trim());
            aiGatewayClient.patch()
                    .uri("/api/v1/admin/flagged-symptoms/{flagId}", flagId.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Không thể gắn mã phiếu {} vào flag {}: {}", ticketNumber, flagId, ex.getMessage());
        }
    }

    @Transactional
    public IntakeResponse processIntake(AdministrativeIntakeRequest request) {
        if (request.getInsuranceCode() != null) {
            String cleanedBhyt = cleanInsuranceCode(request.getInsuranceCode());
            if (cleanedBhyt != null && !cleanedBhyt.matches("^[A-Z0-9]{15}$")) {
                throw new IllegalArgumentException("Mã số thẻ BHYT phải gồm đúng 15 ký tự chữ in hoa và số.");
            }
            request.setInsuranceCode(cleanedBhyt);
        }
        if (request.getIdentityCardNumber() != null && !request.getIdentityCardNumber().isBlank()) {
            String cleanedCccd = request.getIdentityCardNumber().trim();
            if (!cleanedCccd.matches("^\\d{12}$")) {
                throw new IllegalArgumentException("Số CCCD phải gồm đúng 12 chữ số.");
            }
            request.setIdentityCardNumber(cleanedCccd);
        }
        if (request.getInsuranceCode() == null || request.getInsuranceCode().isBlank()) {
            request.setIsOcrVerified(false);
        }

        Patient patient = resolvePatient(request);
        LocalDateTime nowInClinic = LocalDateTime.now(CLINIC_ZONE);
        LocalDate todayInClinic = nowInClinic.toLocalDate();
        LocalTime timeInClinic = nowInClinic.toLocalTime();

        // Chặn tạo trùng nếu đang có phiếu WAITING, CALLED hoặc IN_CONSULTATION trong ngày
        List<QueueStatus> activeStatuses = List.of(
                QueueStatus.WAITING,
                QueueStatus.CALLED,
                QueueStatus.IN_CONSULTATION
        );
        boolean hasActiveBooking = queueTicketRepository.existsActiveTicketForPatientOnDate(
                patient.getId(),
                request.getAppointmentDate(),
                activeStatuses
        );
        if (hasActiveBooking) {
            throw new IllegalStateException(String.format(
                    "Bệnh nhân '%s' đã có phiếu khám đang hoạt động vào ngày %s. Vui lòng hủy phiếu hiện tại trước khi đặt lịch mới.",
                    patient.getFullName(),
                    request.getAppointmentDate()
            ));
        }

        if (request.getAppointmentDate().isBefore(todayInClinic)) {
            throw new IllegalArgumentException(
                    "Ngày khám đã chọn ở trong quá khứ. Vui lòng chọn từ hôm nay trở đi."
            );
        }
        if (request.getAppointmentDate().isEqual(todayInClinic)
                && !request.getSlotStartTime().plusHours(1).isAfter(timeInClinic)) {
            throw new IllegalArgumentException(
                    "Khung giờ khám này trong hôm nay đã kết thúc. Vui lòng chọn khung giờ chiều còn trống hoặc chuyển sang ngày mai."
            );
        }

        String cleanedFlagId = (request.getTriageFlagId() != null && !request.getTriageFlagId().isBlank())
                ? request.getTriageFlagId().trim()
                : null;
        boolean isOutOfDataset = Boolean.TRUE.equals(request.getIsOutOfDataset()) || cleanedFlagId != null;

        boolean isOnlineBooking = request.getIntakeSource() == IntakeSource.ONLINE_BOOKING;
        QueueStatus initialQueueStatus = isOnlineBooking
                ? QueueStatus.PENDING_APPROVAL
                : QueueStatus.WAITING;
        LocalDateTime holdExpiresAt = isOnlineBooking
                ? nowInClinic.plusMinutes(3)
                : null;

        QueueTicket savedTicket = null;
        ScheduleAssignDoctorResponse doctorAssignment = null;
        int attempts = 0;

        while (attempts < MAX_TICKET_GEN_RETRIES) {
            attempts++;
            String ticketNumber = generateTicketNumber(request.getDepartmentId(), request.getAppointmentDate(), attempts - 1);
            boolean slotAssigned = false;

            try {
                doctorAssignment = scheduleClient.assignDoctor(
                        ScheduleAssignDoctorRequest.builder()
                                .departmentId(request.getDepartmentId())
                                .appointmentDate(request.getAppointmentDate())
                                .slotStartTime(request.getSlotStartTime())
                                .preferredDoctorId(request.getPreferredDoctorId())
                                .ticketNumber(ticketNumber)
                                .patientName(patient.getFullName())
                                .patientPhone(patient.getPhone())
                                .insuranceCode(patient.getInsuranceCode())
                                .chiefComplaint(request.getChiefComplaint())
                                .build()
                );
                slotAssigned = true;

                QueueTicket ticket = QueueTicket.builder()
                        .ticketNumber(ticketNumber)
                        .patient(patient)
                        .departmentId(request.getDepartmentId())
                        .departmentName(request.getDepartmentName())
                        .doctorId(doctorAssignment.getDoctorId())
                        .doctorName(doctorAssignment.getDoctorName())
                        .roomNumber(doctorAssignment.getRoomNumber())
                        .appointmentDate(request.getAppointmentDate())
                        .slotStartTime(request.getSlotStartTime())
                        .priorityLevel(request.getPriorityLevel())
                        .intakeSource(request.getIntakeSource())
                        .status(initialQueueStatus)
                        .holdExpiresAt(holdExpiresAt)
                        .chiefComplaint(request.getChiefComplaint())
                        .secondaryDepartmentId(request.getSecondaryDepartmentId())
                        .secondaryDepartmentName(request.getSecondaryDepartmentName())
                        .multiSpecialtyNote(request.getMultiSpecialtyNote())
                        .receiptNumber(request.getReceiptNumber() != null ? request.getReceiptNumber().trim() : null)
                        .receiptAmount(request.getReceiptAmount())
                        .isReceiptVerified(Boolean.TRUE.equals(request.getIsReceiptVerified()))
                        .triageFlagId(cleanedFlagId)
                        .isOutOfDataset(isOutOfDataset)
                        .flagReason(request.getFlagReason())
                        .build();

                savedTicket = queueTicketRepository.saveAndFlush(ticket);
                break;

            } catch (DataIntegrityViolationException ex) {
                if (slotAssigned) {
                    scheduleClient.releaseSlot(request.getAppointmentDate(), ticketNumber);
                }
                log.warn("Đụng độ mã số vé {} tại khoa {} ngày {}. Đang thử lại lần {}...",
                        ticketNumber, request.getDepartmentId(), request.getAppointmentDate(), attempts);
                if (attempts >= MAX_TICKET_GEN_RETRIES) {
                    throw new IllegalStateException("Hệ thống tiếp đón đang quá tải lượt cấp số. Vui lòng thử lại sau giây lát.");
                }
            } catch (Exception ex) {
                if (slotAssigned) {
                    scheduleClient.releaseSlot(request.getAppointmentDate(), ticketNumber);
                }
                log.error("Lỗi trong quá trình tiếp nhận bệnh nhân {}: {}", patient.getFullName(), ex.getMessage());
                throw ex;
            }
        }

        if (cleanedFlagId != null) {
            linkFlagToTicket(
                    cleanedFlagId,
                    savedTicket.getTicketNumber(),
                    patient.getFullName(),
                    savedTicket.getFlagReason(),
                    isOutOfDataset
            );
        }

        log.info("Tiếp nhận thành công BN: {}, Mã phiếu: {}, Kênh: {}, Trạng thái: {}",
                patient.getFullName(), savedTicket.getTicketNumber(), request.getIntakeSource(),
                savedTicket.getStatus());

        return IntakeResponse.builder()
                .ticketNumber(savedTicket.getTicketNumber())
                .patientId(patient.getId())
                .patientName(patient.getFullName())
                .insuranceCode(patient.getInsuranceCode())
                .departmentId(savedTicket.getDepartmentId())
                .departmentName(savedTicket.getDepartmentName())
                .doctorId(savedTicket.getDoctorId())
                .doctorName(savedTicket.getDoctorName())
                .roomNumber(savedTicket.getRoomNumber())
                .appointmentDate(savedTicket.getAppointmentDate())
                .slotStartTime(savedTicket.getSlotStartTime())
                .priorityLevel(savedTicket.getPriorityLevel())
                .checkInTime(savedTicket.getCheckInTime())
                .message("Tiếp nhận thành công. Vui lòng di chuyển đến phòng khám được chỉ định.")
                .secondaryDepartmentId(savedTicket.getSecondaryDepartmentId())
                .secondaryDepartmentName(savedTicket.getSecondaryDepartmentName())
                .multiSpecialtyNote(savedTicket.getMultiSpecialtyNote())
                .receiptNumber(savedTicket.getReceiptNumber())
                .receiptAmount(savedTicket.getReceiptAmount())
                .isReceiptVerified(Boolean.TRUE.equals(savedTicket.getIsReceiptVerified()))
                .triageFlagId(savedTicket.getTriageFlagId())
                .isOutOfDataset(Boolean.TRUE.equals(savedTicket.getIsOutOfDataset()))
                .flagReason(savedTicket.getFlagReason())
                .status(savedTicket.getStatus())
                .build();
    }

    @Transactional
    public QueueTicket updateTicketStatus(String ticketNumber, QueueStatus status) {
        QueueTicket ticket = queueTicketRepository
                .findByAppointmentDateAndTicketNumber(LocalDate.now(CLINIC_ZONE), ticketNumber)
                .or(() -> queueTicketRepository.findTopByTicketNumberOrderByAppointmentDateDesc(ticketNumber))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu khám có mã: " + ticketNumber));

        ticket.setStatus(status);
        QueueTicket saved = queueTicketRepository.save(ticket);
        log.info("Cập nhật trạng thái phiếu {} sang {}", ticketNumber, status);

        if (status == QueueStatus.CANCELLED) {
            scheduleClient.releaseSlot(ticket.getAppointmentDate(), ticket.getTicketNumber());
        }

        return saved;
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public QueueTicketDetailResponse getActiveTicketByPatientId(Long patientId) {
        List<QueueTicket> tickets = queueTicketRepository.findActiveTicketsByPatientId(
                patientId,
                List.of(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.IN_CONSULTATION)
        );

        if (tickets.isEmpty()) {
            throw new IllegalArgumentException("Bệnh nhân hiện không có phiếu khám nào đang hoạt động.");
        }

        QueueTicket ticket = tickets.get(0);
        LocalDateTime nowInClinic = LocalDateTime.now(CLINIC_ZONE);

        if (ticket.getAppointmentDate().isBefore(nowInClinic.toLocalDate())) {
            ticket.setStatus(QueueStatus.CANCELLED);
            queueTicketRepository.save(ticket);
            scheduleClient.releaseSlot(ticket.getAppointmentDate(), ticket.getTicketNumber());
            throw new IllegalArgumentException("Phiếu khám trước đó đã hết hạn do qua ngày hẹn.");
        }

        return mapToDetailResponse(ticket, ticket.getPatient());
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public QueueTicketDetailResponse getActiveTicketByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Thông tin định danh bệnh nhân không được để trống.");
        }

        List<QueueTicket> tickets = queueTicketRepository.findActiveTicketsByIdentifier(
                identifier.trim(),
                List.of(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.IN_CONSULTATION)
        );

        if (tickets.isEmpty()) {
            throw new IllegalArgumentException("Bệnh nhân hiện không có phiếu khám nào đang hoạt động.");
        }

        QueueTicket ticket = tickets.get(0);
        LocalDateTime nowInClinic = LocalDateTime.now(CLINIC_ZONE);

        if (ticket.getAppointmentDate().isBefore(nowInClinic.toLocalDate())) {
            ticket.setStatus(QueueStatus.CANCELLED);
            queueTicketRepository.save(ticket);
            scheduleClient.releaseSlot(ticket.getAppointmentDate(), ticket.getTicketNumber());
            log.info("Phiếu {} ngày {} đã quá hạn so với hôm nay, tự động chuyển sang CANCELLED",
                    ticket.getTicketNumber(), ticket.getAppointmentDate());
            return null;
        }

        return mapToDetailResponse(ticket, ticket.getPatient());
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
                .build();
    }
}