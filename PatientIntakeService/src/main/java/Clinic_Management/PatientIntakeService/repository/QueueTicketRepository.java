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

    @Query("SELECT q FROM QueueTicket q JOIN FETCH q.patient WHERE q.ticketNumber = :ticketNumber")
    Optional<QueueTicket> findByTicketNumberWithPatient(@Param("ticketNumber") String ticketNumber);

    Optional<QueueTicket> findByTicketNumber(String ticketNumber);
    List<QueueTicket> findByDepartmentIdAndAppointmentDateAndStatusIn(Long departmentId, LocalDate appointmentDate, List<QueueStatus> statuses);
    List<QueueTicket> findByAppointmentDateAndStatusIn(LocalDate appointmentDate, List<QueueStatus> statuses);
}