package Clinic_Management.DoctorScheduleService.repository;

import Clinic_Management.DoctorScheduleService.entity.SlotAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface SlotAssignmentRepository extends JpaRepository<SlotAssignment, Long> {

    @Query("""
        SELECT COUNT(sa) FROM SlotAssignment sa
        WHERE sa.doctor.id = :doctorId
          AND sa.appointmentDate = :appointmentDate
          AND sa.slotStartTime = :slotStartTime
          AND sa.status = 'BOOKED'
    """)
    long countBookedByDoctorAndSlot(
            @Param("doctorId") Long doctorId,
            @Param("appointmentDate") LocalDate appointmentDate,
            @Param("slotStartTime") LocalTime slotStartTime
    );

    @Query("""
        SELECT COUNT(sa) FROM SlotAssignment sa
        WHERE sa.departmentId = :departmentId
          AND sa.appointmentDate = :appointmentDate
          AND sa.slotStartTime = :slotStartTime
          AND sa.status = 'BOOKED'
    """)
    long countBookedByDepartmentAndSlot(
            @Param("departmentId") Long departmentId,
            @Param("appointmentDate") LocalDate appointmentDate,
            @Param("slotStartTime") LocalTime slotStartTime
    );

    List<SlotAssignment> findByTicketNumber(String ticketNumber);

    List<SlotAssignment> findByAppointmentDateAndTicketNumber(LocalDate appointmentDate, String ticketNumber);

    List<SlotAssignment> findByDepartmentIdAndAppointmentDateAndSlotStartTime(
            Long departmentId, LocalDate appointmentDate, LocalTime slotStartTime
    );
}