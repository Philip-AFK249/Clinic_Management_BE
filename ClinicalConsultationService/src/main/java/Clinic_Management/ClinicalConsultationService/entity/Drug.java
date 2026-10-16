package Clinic_Management.ClinicalConsultationService.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "drugs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Drug {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code; // DRG-001

    @Column(nullable = false, length = 150)
    private String name; // Amoxicillin

    @Column(nullable = false, length = 50)
    private String concentration; //  500mg

    @Column(nullable = false, length = 50)
    private String dosageForm; // capsule, tablet, syrup, injection

    @Column(nullable = false)
    private Integer stockQuantity; // count of drug in stock

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice; // price per unit of drug

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BhytCoverage bhytCoverage; // BHYT coverage for the drug

    @Builder.Default
    @Column(nullable = false)
    private Boolean isPenicillinClass = false; // identify if the drug is in the penicillin class

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;
}