package Clinic_Management.ClinicalConsultationService.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "prescription_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_id", nullable = false)
    private Drug drug;

    @Column(name = "drug_name", nullable = false)
    private String drugName;

    @Column(name = "dosage_form", nullable = false)
    private String dosageForm; // Dosage form

    @Column(name = "unit", length = 30)
    private String unit; // Đơn vị tính: Viên, Lọ, Ống...

    @Column(name = "route_frequency", nullable = false)
    private String routeFrequency; // 1 times per day, 2 times per day, 3 times per day, 4 times per day

    @Column(nullable = false)
    private String duration; // 7 days, 10 days, 14 days, 21 days, 28 days

    @Column(nullable = false)
    private Integer quantity; // number of units to be dispensed

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount; // quantity * unitPrice
}