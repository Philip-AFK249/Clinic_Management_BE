package Clinic_Management.DoctorScheduleService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AssignDoctorRequest {
    @NotNull(message = "ID chuyên khoa không được để trống")
    private Long departmentId;

    @NotNull(message = "Ngày khám không được để trống")
    private LocalDate appointmentDate;

    @NotNull(message = "Khung giờ khám không được để trống")
    private LocalTime slotStartTime;

    @NotBlank(message = "Mã số phiếu không được để trống")
    private String ticketNumber;
    private Long preferredDoctorId;
    private String patientName;
    private String patientPhone;
    private String insuranceCode;
    private String chiefComplaint;
}