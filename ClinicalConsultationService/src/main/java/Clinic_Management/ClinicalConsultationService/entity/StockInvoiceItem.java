package Clinic_Management.ClinicalConsultationService.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "stock_invoice_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockInvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_invoice_id", nullable = false)
    private StockInvoice stockInvoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_id", nullable = false)
    private Drug drug;

    @Column(name = "ocr_drug_name", length = 200)
    private String ocrDrugName;

    @Column(name = "batch_number", length = 60)
    private String batchNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "quantity_added", nullable = false)
    private Integer quantityAdded;

    @Column(name = "import_unit_price", precision = 12, scale = 2)
    private BigDecimal importUnitPrice;
}