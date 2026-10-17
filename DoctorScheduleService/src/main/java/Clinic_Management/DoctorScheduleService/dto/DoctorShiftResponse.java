package Clinic_Management.DoctorScheduleService.dto;

import Clinic_Management.DoctorScheduleService.entity.DutyType;
import Clinic_Management.DoctorScheduleService.entity.ShiftSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorShiftResponse {
    private Long id;
    private DoctorReference doctor;
    private Long departmentId;
    private String departmentName;
    private LocalDate shiftDate;
    private ShiftSession session;
    private DutyType dutyType;
    private Integer maxPatientsPerSlot;
    private String roomNumber;
    private Long nurseId;
    private String nurseName;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorReference {
        private Long id;
        private String fullName;
        private String title;
        private String roomNumber;
    }
}