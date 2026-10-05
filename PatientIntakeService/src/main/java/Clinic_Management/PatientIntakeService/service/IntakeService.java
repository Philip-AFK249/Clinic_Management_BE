package Clinic_Management.PatientIntakeService.service;

import Clinic_Management.PatientIntakeService.clients.ScheduleServiceClient;
import Clinic_Management.PatientIntakeService.dto.AdministrativeIntakeRequest;
import Clinic_Management.PatientIntakeService.dto.IntakeResponse;
import Clinic_Management.PatientIntakeService.dto.ScheduleAssignDoctorRequest;
import Clinic_Management.PatientIntakeService.dto.ScheduleAssignDoctorResponse;
import Clinic_Management.PatientIntakeService.entity.Patient;
import Clinic_Management.PatientIntakeService.entity.QueueStatus;
import Clinic_Management.PatientIntakeService.entity.QueueTicket;
import Clinic_Management.PatientIntakeService.repository.PatientRepository;
import Clinic_Management.PatientIntakeService.repository.QueueTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntakeService {

    private final PatientRepository patientRepository;
    private final QueueTicketRepository queueTicketRepository;
    private final ScheduleServiceClient scheduleClient;

    private static final int MAX_TICKET_GEN_RETRIES = 3;

    @Transactional
    public IntakeResponse processIntake(AdministrativeIntakeRequest request) {
        if (request.getInsuranceCode() != null) {
            request.setInsuranceCode(cleanInsuranceCode(request.getInsuranceCode()));
        }

        Patient patient = resolvePatient(request);

        QueueTicket savedTicket = null;
        ScheduleAssignDoctorResponse doctorAssignment = null;
        int attempts = 0;

        while (attempts < MAX_TICKET_GEN_RETRIES) {
            attempts++;
            String ticketNumber = generateTicketNumber(request.getDepartmentId(), request.getAppointmentDate(), attempts - 1);

            try {
                doctorAssignment = scheduleClient.assignDoctor(
                        ScheduleAssignDoctorRequest.builder()
                                .departmentId(request.getDepartmentId())
                                .appointmentDate(request.getAppointmentDate())
                                .slotStartTime(request.getSlotStartTime())
                                .ticketNumber(ticketNumber)
                                .patientName(patient.getFullName())
                                .patientPhone(patient.getPhone())
                                .insuranceCode(patient.getInsuranceCode())
                                .chiefComplaint(request.getChiefComplaint())
                                .build()
                );

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
                        .status(QueueStatus.WAITING)
                        .chiefComplaint(request.getChiefComplaint())
                        .build();

                savedTicket = queueTicketRepository.saveAndFlush(ticket);
                break;

            } catch (DataIntegrityViolationException ex) {
                log.warn("Đụng độ mã số vé {} tại khoa {} ngày {}. Đang thử lại lần {}...",
                        ticketNumber, request.getDepartmentId(), request.getAppointmentDate(), attempts);
                if (attempts >= MAX_TICKET_GEN_RETRIES) {
                    throw new IllegalStateException("Hệ thống tiếp đón đang quá tải lượt cấp số. Vui lòng thử lại sau giây lát.");
                }
            } catch (Exception ex) {
                log.error("Lỗi trong quá trình tiếp nhận bệnh nhân {}: {}", patient.getFullName(), ex.getMessage());
                throw ex;
            }
        }

        log.info("Tiếp nhận thành công BN: {}, Mã phiếu: {}, Phòng khám: {}",
                patient.getFullName(), savedTicket.getTicketNumber(), savedTicket.getRoomNumber());

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
        if (req.getInsuranceCode() != null && !req.getInsuranceCode().isBlank()) {
            patient.setInsuranceCode(cleanInsuranceCode(req.getInsuranceCode()));
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
        if (Boolean.TRUE.equals(req.getIsOcrVerified())) patient.setIsOcrVerified(true);
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
}