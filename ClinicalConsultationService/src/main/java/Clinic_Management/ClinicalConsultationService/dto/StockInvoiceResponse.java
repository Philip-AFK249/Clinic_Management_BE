package Clinic_Management.ClinicalConsultationService.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class StockInvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private String supplierName;
    private LocalDate invoiceDate;
    private BigDecimal totalAmount;
    private String verifiedBy;
    private String notes;
    private LocalDateTime createdAt;
    private List<StockInvoiceItemResponse> items;

    @Data
    @Builder
    public static class StockInvoiceItemResponse {
        private Long itemId;
        private Long drugId;
        private String drugCode;
        private String drugName;
        private String ocrDrugName;
        private String batchNumber;
        private LocalDate expiryDate;
        private Integer quantityAdded;
        private Integer newStockQuantity;
        private BigDecimal importUnitPrice;
    }
}