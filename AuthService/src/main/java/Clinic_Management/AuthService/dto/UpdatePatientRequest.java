package Clinic_Management.AuthService.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdatePatientRequest {
    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    private String phone;
    private String identityCardNumber;
    private String insuranceCode;
    private String initialHospitalCode;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private Boolean isOcrVerified;
    private LocalDate validFrom;
    private LocalDate validUntil;

}