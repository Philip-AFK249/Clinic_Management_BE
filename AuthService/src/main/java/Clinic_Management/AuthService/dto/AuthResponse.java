package Clinic_Management.AuthService.dto;
import Clinic_Management.AuthService.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long userId;
    private String email;
    private String fullName;
    private String phone;
    private UserRole role;
    private Boolean active;

    private Long departmentId;
    private String departmentName;
    private Long doctorId;
    private Long pharmacistId;

    private String identityCardNumber;
    private String insuranceCode;
    private String initialHospitalCode;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private Boolean isOcrVerified;

    private LocalDateTime createdAt;
    private LocalDate validFrom;
    private LocalDate validUntil;
}