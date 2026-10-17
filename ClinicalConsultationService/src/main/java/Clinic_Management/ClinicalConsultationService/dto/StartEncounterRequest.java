package Clinic_Management.ClinicalConsultationService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartEncounterRequest {
    @NotBlank(message = "Mã số phiếu không được để trống")
    private String ticketNumber;

    @NotNull(message = "ID Bệnh nhân không được để trống")
    private Long patientId;

    @NotBlank(message = "Tên bệnh nhân không được để trống")
    private String patientName;

    @NotNull(message = "ID Bác sĩ không được để trống")
    private Long doctorId;

    @NotBlank(message = "Tên bác sĩ không được để trống")
    private String doctorName;

    @NotNull(message = "ID Chuyên khoa không được để trống")
    private Long departmentId;

    private String insuranceCode;
    private String bloodPressure;
    private Integer heartRate;
    private String temperature;
    private Integer spo2;
}