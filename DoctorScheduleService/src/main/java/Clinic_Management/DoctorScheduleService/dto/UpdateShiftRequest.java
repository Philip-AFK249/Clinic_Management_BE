package Clinic_Management.DoctorScheduleService.dto;

import Clinic_Management.DoctorScheduleService.entity.DutyType;
import Clinic_Management.DoctorScheduleService.entity.ShiftSession;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateShiftRequest {
    private Long doctorId;
    private Long departmentId;
    private LocalDate shiftDate;
    private ShiftSession session;
    private DutyType dutyType;
    private Integer maxPatientsPerSlot;
    private Long nurseId;
    private String nurseName;
}