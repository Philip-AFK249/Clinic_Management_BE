package Clinic_Management.DoctorScheduleService.repository;

import Clinic_Management.DoctorScheduleService.entity.SlotAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface SlotAssignmentRepository extends JpaRepository<SlotAssignment, Long> {

    @Query("""
        SELECT COUNT(sa) FROM SlotAssignment sa
        WHERE sa.doctor.id = :doctorId
          AND sa.appointmentDate = :appointmentDate
          AND sa.slotStartTime = :slotStartTime
          AND (
              sa.status = 'BOOKED'
              OR (sa.status = 'TEMPORARY' AND (sa.expiresAt IS NULL OR sa.expiresAt > :now))
          )
    """)
    long countActiveByDoctorAndSlot(
            @Param("doctorId") Long doctorId,
            @Param("appointmentDate") LocalDate appointmentDate,
            @Param("slotStartTime") LocalTime slotStartTime,
            @Param("now") LocalDateTime now
    );

    @Query("""
        SELECT COUNT(sa) FROM SlotAssignment sa
        WHERE sa.departmentId = :departmentId
          AND sa.appointmentDate = :appointmentDate
          AND sa.slotStartTime = :slotStartTime
          AND (
              sa.status = 'BOOKED'
              OR (sa.status = 'TEMPORARY' AND (sa.expiresAt IS NULL OR sa.expiresAt > :now))
          )
    """)
    long countActiveByDepartmentAndSlot(
            @Param("departmentId") Long departmentId,
            @Param("appointmentDate") LocalDate appointmentDate,
            @Param("slotStartTime") LocalTime slotStartTime,
            @Param("now") LocalDateTime now
    );

    List<SlotAssignment> findByTicketNumber(String ticketNumber);

    List<SlotAssignment> findByDepartmentIdAndAppointmentDateAndSlotStartTime(
            Long departmentId, LocalDate appointmentDate, LocalTime slotStartTime
    );

    @Modifying
    @Query("""
        UPDATE SlotAssignment sa
        SET sa.status = 'CANCELLED', sa.expiresAt = NULL
        WHERE sa.status = 'TEMPORARY'
          AND sa.expiresAt IS NOT NULL
          AND sa.expiresAt <= :now
    """)
    int releaseExpiredTemporaryAssignments(@Param("now") LocalDateTime now);
}