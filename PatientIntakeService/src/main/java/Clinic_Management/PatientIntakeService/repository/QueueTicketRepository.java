package Clinic_Management.PatientIntakeService.repository;

import Clinic_Management.PatientIntakeService.entity.QueueStatus;
import Clinic_Management.PatientIntakeService.entity.QueueTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueTicketRepository extends JpaRepository<QueueTicket, Long> {

    @Query("SELECT COUNT(q) FROM QueueTicket q WHERE q.appointmentDate = :date AND q.departmentId = :departmentId")
    long countByAppointmentDateAndDepartmentId(
            @Param("date") LocalDate date,
            @Param("departmentId") Long departmentId
    );

    @Query("""
        SELECT q FROM QueueTicket q
        JOIN FETCH q.patient
        WHERE q.doctorId = :doctorId
          AND q.appointmentDate = :date
          AND q.status IN :statuses
        ORDER BY
          CASE q.priorityLevel WHEN 'P1' THEN 1 ELSE 2 END ASC,
          q.slotStartTime ASC,
          CASE q.priorityLevel WHEN 'P2' THEN 1 WHEN 'P3' THEN 2 END ASC,
          q.checkInTime ASC
    """)
    List<QueueTicket> findDoctorActiveQueue(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("statuses") List<QueueStatus> statuses
    );

    @Query("""
        SELECT q FROM QueueTicket q
        JOIN FETCH q.patient
        WHERE q.departmentId = :departmentId
          AND q.appointmentDate = :appointmentDate
          AND q.status = :status
        ORDER BY q.slotStartTime ASC, q.checkInTime ASC
    """)
    List<QueueTicket> findByDepartmentIdAndAppointmentDateAndStatus(
            @Param("departmentId") Long departmentId,
            @Param("appointmentDate") LocalDate appointmentDate,
            @Param("status") QueueStatus status
    );

    // Tìm vé kèm thông tin bệnh nhân - sắp xếp lấy ngày mới nhất tránh lỗi 500 IncorrectResultSize
    @Query("""
        SELECT q FROM QueueTicket q 
        JOIN FETCH q.patient 
        WHERE q.ticketNumber = :ticketNumber 
        ORDER BY q.appointmentDate DESC
    """)
    List<QueueTicket> findByTicketNumberWithPatientOrderByDateDesc(@Param("ticketNumber") String ticketNumber);

    Optional<QueueTicket> findTopByTicketNumberOrderByAppointmentDateDesc(String ticketNumber);
    Optional<QueueTicket> findByAppointmentDateAndTicketNumber(LocalDate appointmentDate, String ticketNumber);

    // LẤY VÉ ĐANG HOẠT ĐỘNG: Tìm từ ngày hôm nay trở đi (>= CURRENT_DATE) thay vì chỉ đúng hôm nay (= CURRENT_DATE)
    @Query("""
        SELECT q FROM QueueTicket q
        JOIN FETCH q.patient
        WHERE q.patient.id = :patientId
          AND q.appointmentDate >= CURRENT_DATE
          AND q.status IN :statuses
        ORDER BY q.appointmentDate ASC, q.slotStartTime ASC
    """)
    List<QueueTicket> findActiveTicketsByPatientId(
            @Param("patientId") Long patientId,
            @Param("statuses") List<QueueStatus> statuses
    );

    // LẤY VÉ THEO IDENTIFIER: Tìm từ hôm nay trở đi (>= CURRENT_DATE)
    @Query("""
        SELECT q FROM QueueTicket q
        JOIN FETCH q.patient
        WHERE (q.patient.phone = :identifier 
            OR q.patient.identityCardNumber = :identifier 
            OR q.patient.insuranceCode = :identifier)
          AND q.appointmentDate >= CURRENT_DATE
          AND q.status IN :statuses
        ORDER BY q.appointmentDate ASC, q.slotStartTime ASC
    """)
    List<QueueTicket> findActiveTicketsByIdentifier(
            @Param("identifier") String identifier,
            @Param("statuses") List<QueueStatus> statuses
    );

    @Query("""
        SELECT q FROM QueueTicket q
        JOIN FETCH q.patient
        WHERE q.doctorId = :doctorId
          AND q.appointmentDate >= :fromDate
          AND q.status IN :statuses
        ORDER BY
          q.appointmentDate ASC,
          CASE q.priorityLevel WHEN 'P1' THEN 1 ELSE 2 END ASC,
          q.slotStartTime ASC,
          q.checkInTime ASC
    """)
    List<QueueTicket> findDoctorUpcomingQueue(
            @Param("doctorId") Long doctorId,
            @Param("fromDate") LocalDate fromDate,
            @Param("statuses") List<QueueStatus> statuses
    );

    @Query("""
        SELECT q FROM QueueTicket q
        JOIN FETCH q.patient
        WHERE q.departmentId = :departmentId
          AND q.appointmentDate = :appointmentDate
          AND q.status IN :statuses
        ORDER BY
          CASE q.priorityLevel WHEN 'P1' THEN 1 ELSE 2 END ASC,
          q.slotStartTime ASC,
          q.checkInTime ASC
    """)
    List<QueueTicket> findByDepartmentIdAndAppointmentDateAndStatusIn(
            @Param("departmentId") Long departmentId,
            @Param("appointmentDate") LocalDate appointmentDate,
            @Param("statuses") List<QueueStatus> statuses
    );

    @Query("""
        SELECT q FROM QueueTicket q
        JOIN FETCH q.patient
        WHERE q.appointmentDate = :appointmentDate
          AND q.status IN :statuses
        ORDER BY
          q.departmentId ASC,
          CASE q.priorityLevel WHEN 'P1' THEN 1 ELSE 2 END ASC,
          q.slotStartTime ASC,
          q.checkInTime ASC
    """)
    List<QueueTicket> findByAppointmentDateAndStatusIn(
            @Param("appointmentDate") LocalDate appointmentDate,
            @Param("statuses") List<QueueStatus> statuses
    );

    @Query("""
        SELECT COUNT(q) > 0 FROM QueueTicket q
        WHERE q.patient.id = :patientId
          AND q.appointmentDate = :appointmentDate
          AND q.status IN :statuses
    """)
    boolean existsActiveTicketForPatientOnDate(
            @Param("patientId") Long patientId,
            @Param("appointmentDate") LocalDate appointmentDate,
            @Param("statuses") List<QueueStatus> statuses
    );
}