package Clinic_Management.PatientIntakeService.dto;

import Clinic_Management.PatientIntakeService.entity.IntakeSource;
import Clinic_Management.PatientIntakeService.entity.TriagePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AdministrativeIntakeRequest {
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    private String identityCardNumber;
    private String insuranceCode;
    private String initialHospitalCode;
    private LocalDate dateOfBirth;
    private String gender;
    private String phone;
    private String address;
    private Boolean isOcrVerified = false;

    @NotNull(message = "Chuyên khoa khám không được để trống")
    private Long departmentId;

    @NotBlank(message = "Tên chuyên khoa không được để trống")
    private String departmentName;

    private Long preferredDoctorId;

    @NotNull(message = "Ngày khám không được để trống")
    private LocalDate appointmentDate;

    @NotNull(message = "Khung giờ slot không được để trống")
    private LocalTime slotStartTime;

    @NotNull(message = "Độ ưu tiên không được để trống")
    private TriagePriority priorityLevel;

    @NotNull(message = "Nguồn tiếp nhận không được để trống")
    private IntakeSource intakeSource;

    private String chiefComplaint;

    private Long secondaryDepartmentId;
    private String secondaryDepartmentName;
    private String multiSpecialtyNote;
}