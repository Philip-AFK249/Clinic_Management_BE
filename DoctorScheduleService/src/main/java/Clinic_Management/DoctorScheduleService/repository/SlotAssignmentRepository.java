package Clinic_Management.DoctorScheduleService.repository;

import Clinic_Management.DoctorScheduleService.entity.SlotAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface SlotAssignmentRepository extends JpaRepository<SlotAssignment, Long> {

    long countByDoctorIdAndAppointmentDateAndSlotStartTimeAndStatus(
            Long doctorId, LocalDate appointmentDate, LocalTime slotStartTime, String status
    );

    long countByDepartmentIdAndAppointmentDateAndSlotStartTimeAndStatus(
            Long departmentId, LocalDate appointmentDate, LocalTime slotStartTime, String status
    );

    List<SlotAssignment> findByTicketNumber(String ticketNumber);

    List<SlotAssignment> findByDepartmentIdAndAppointmentDateAndSlotStartTime(
            Long departmentId, LocalDate appointmentDate, LocalTime slotStartTime
    );
}