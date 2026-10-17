package Clinic_Management.DoctorScheduleService.dto;
import Clinic_Management.DoctorScheduleService.entity.DutyType;
import Clinic_Management.DoctorScheduleService.entity.ShiftSession;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateShiftRequest {
    @NotNull(message = "ID Bác sĩ không được để trống")
    private Long doctorId;

    @NotNull(message = "ID Khoa không được để trống")
    private Long departmentId;

    @NotNull(message = "Ngày trực không được để trống")
    @FutureOrPresent(message = "Ngày trực phải là hiện tại hoặc tương lai")
    private LocalDate shiftDate;

    @NotNull(message = "Buổi trực không được để trống")
    private ShiftSession session;

    @NotNull(message = "Loại ca trực không được để trống")
    private DutyType dutyType;

    private Long nurseId;
    private String nurseName;
}