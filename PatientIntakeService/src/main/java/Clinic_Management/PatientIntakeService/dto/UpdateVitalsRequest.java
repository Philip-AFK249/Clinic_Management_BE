package Clinic_Management.PatientIntakeService.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateVitalsRequest {
    @NotBlank(message = "Huyết áp không được để trống")
    private String bloodPressure;

    private Integer heartRate;
    private String temperature;
    private Integer spo2;
    private Double weightKg;
    private Double heightCm;
    private String nurseNotes;
}