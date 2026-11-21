package Clinic_Management.PatientIntakeService.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patients", indexes = {
        @Index(name = "idx_patient_identity", columnList = "identity_card_number"),
        @Index(name = "idx_patient_insurance", columnList = "insurance_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "identity_card_number", length = 20)
    private String identityCardNumber;

    @Column(name = "insurance_code", length = 30)
    private String insuranceCode;

    @Column(name = "initial_hospital_code")
    private String initialHospitalCode;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 10)
    private String gender;

    @Column(length = 20)
    private String phone;

    private String address;

    @Builder.Default
    @Column(name = "is_ocr_verified", nullable = false)
    private Boolean isOcrVerified = false;

    @Column(name = "booking_locked_until")
    private LocalDateTime bookingLockedUntil;

    @Column(name = "booking_lock_reason", columnDefinition = "TEXT")
    private String bookingLockReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}