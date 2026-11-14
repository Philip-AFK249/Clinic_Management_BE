package Clinic_Management.DoctorScheduleService.repository;

import Clinic_Management.DoctorScheduleService.entity.DoctorShift;
import Clinic_Management.DoctorScheduleService.entity.DutyType;
import Clinic_Management.DoctorScheduleService.entity.ShiftSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DoctorShiftRepository extends JpaRepository<DoctorShift, Long> {
    List<DoctorShift> findByDepartmentIdAndShiftDate(Long departmentId, LocalDate shiftDate);

    List<DoctorShift> findByDepartmentIdAndShiftDateAndDutyType(
            Long departmentId, LocalDate shiftDate, DutyType dutyType
    );

    long countByDepartmentIdAndShiftDateAndSessionAndDutyType(
            Long departmentId, LocalDate shiftDate, ShiftSession session, DutyType dutyType
    );

    List<DoctorShift> findByDepartmentIdAndShiftDateBetweenOrderByShiftDateAsc(
            Long departmentId, LocalDate startDate, LocalDate endDate
    );

    boolean existsByDoctorIdAndShiftDateAndSession(
            Long doctorId, LocalDate shiftDate, ShiftSession session
    );
}