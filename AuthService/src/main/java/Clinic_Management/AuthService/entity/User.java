package Clinic_Management.AuthService.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_role", columnList = "role"),
        @Index(name = "idx_user_insurance", columnList = "insurance_code"),
        @Index(name = "idx_user_identity", columnList = "identity_card_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    // --- Thông tin chuyên khoa & Nhân sự ---
    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "department_name")
    private String departmentName;

    @Column(name = "doctor_id")
    private Long doctorId;

    @Column(name = "pharmacist_id")
    private Long pharmacistId;

    @Column(name = "identity_card_number", length = 20)
    private String identityCardNumber;

    @Column(name = "insurance_code", length = 30)
    private String insuranceCode;

    @Column(name = "initial_hospital_code", length = 150)
    private String initialHospitalCode;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 10)
    private String gender;

    @Column(length = 255)
    private String address;

    @Builder.Default
    @Column(name = "is_ocr_verified")
    private Boolean isOcrVerified = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;
}