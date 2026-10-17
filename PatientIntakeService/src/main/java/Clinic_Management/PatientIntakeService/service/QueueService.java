package Clinic_Management.PatientIntakeService.service;
import Clinic_Management.PatientIntakeService.clients.ScheduleServiceClient;
import Clinic_Management.PatientIntakeService.dto.QueueTicketDetailResponse;
import Clinic_Management.PatientIntakeService.entity.Patient;
import Clinic_Management.PatientIntakeService.entity.QueueStatus;
import Clinic_Management.PatientIntakeService.entity.QueueTicket;
import Clinic_Management.PatientIntakeService.repository.QueueTicketRepository;
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
public class QueueService {

    private final QueueTicketRepository queueTicketRepository;
    private final ScheduleServiceClient scheduleClient;

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

        if (status == QueueStatus.CANCELLED) {
            scheduleClient.releaseSlot(ticketNumber);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public QueueTicketDetailResponse getTicketDetails(String ticketNumber) {
        List<QueueTicket> tickets = queueTicketRepository.findByTicketNumberWithPatientOrderByDateDesc(ticketNumber);
        if (tickets.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy thông tin phiếu khám: " + ticketNumber);
        }
        QueueTicket ticket = tickets.get(0); // Lấy phiếu mới nhất, tránh lỗi 500 do trùng mã khác ngày
        return mapToDetailResponse(ticket, ticket.getPatient());
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

    @Transactional(readOnly = true)
    public QueueTicketDetailResponse getActiveTicketByPatientId(Long patientId) {
        List<QueueTicket> tickets = queueTicketRepository.findActiveTicketsByPatientId(
                patientId,
                List.of(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.IN_CONSULTATION)
        );

        if (tickets.isEmpty()) {
            return null; // Trả về null an toàn thay vì ném 400 Bad Request
        }

        QueueTicket ticket = tickets.get(0);
        return mapToDetailResponse(ticket, ticket.getPatient());
    }

    @Transactional(readOnly = true)
    public QueueTicketDetailResponse getActiveTicketByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }

        List<QueueTicket> tickets = queueTicketRepository.findActiveTicketsByIdentifier(
                identifier.trim(),
                List.of(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.IN_CONSULTATION)
        );

        if (tickets.isEmpty()) {
            return null; // Trả về null để HTTP trả 200/204 thay vì văng 400 đỏ console
        }

        QueueTicket ticket = tickets.get(0);
        return mapToDetailResponse(ticket, ticket.getPatient());
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
                .build();
    }
}