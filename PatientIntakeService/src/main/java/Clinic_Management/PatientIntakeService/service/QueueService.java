package Clinic_Management.PatientIntakeService.service;

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
        log.info("Cập nhật trạng thái phiếu {} sang {}", ticketNumber, status);
        return queueTicketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public QueueTicketDetailResponse getTicketDetails(String ticketNumber) {
        QueueTicket ticket = queueTicketRepository.findByTicketNumberWithPatient(ticketNumber)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin phiếu khám: " + ticketNumber));

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

        // Tự động đóng phiếu quá ngày và lưu vào DB (không bị rollback bởi IllegalArgumentException)
        if (ticket.getAppointmentDate().isBefore(LocalDate.now())) {
            ticket.setStatus(QueueStatus.CANCELLED);
            queueTicketRepository.save(ticket);
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

        if (ticket.getAppointmentDate().isBefore(LocalDate.now())) {
            ticket.setStatus(QueueStatus.CANCELLED);
            queueTicketRepository.save(ticket);
            throw new IllegalArgumentException("Phiếu khám trước đó đã hết hạn do qua ngày hẹn.");
        }

        return mapToDetailResponse(ticket, ticket.getPatient());
    }

    @Transactional(readOnly = true)
    public List<QueueTicket> getDoctorUpcomingQueue(Long doctorId, LocalDate fromDate) {
        return queueTicketRepository.findDoctorUpcomingQueue(
                doctorId, fromDate, List.of(QueueStatus.WAITING, QueueStatus.CALLED, QueueStatus.IN_CONSULTATION)
        );
    }

    // =========================================================================
    // HELPER MAPPING FUNCTION
    // =========================================================================
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
                .patientId(patient.getId())
                .patientFullName(patient.getFullName())
                .identityCardNumber(patient.getIdentityCardNumber())
                .insuranceCode(patient.getInsuranceCode())
                .initialHospitalCode(patient.getInitialHospitalCode())
                .dateOfBirth(patient.getDateOfBirth())
                .gender(patient.getGender())
                .phone(patient.getPhone())
                .address(patient.getAddress())
                .isOcrVerified(patient.getIsOcrVerified())
                .build();
    }
}