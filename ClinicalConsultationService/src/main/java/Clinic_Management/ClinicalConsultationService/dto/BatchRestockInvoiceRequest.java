package Clinic_Management.ClinicalConsultationService.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class BatchRestockInvoiceRequest {

    @NotBlank(message = "Số hóa đơn không được để trống")
    private String invoiceNumber;

    @NotBlank(message = "Tên nhà cung cấp không được để trống")
    private String supplierName;

    private LocalDate invoiceDate;
    private BigDecimal totalAmount;

    @NotBlank(message = "Người xác nhận nhập kho không được để trống")
    private String verifiedBy;

    private String notes;

    @NotEmpty(message = "Hóa đơn phải có ít nhất 1 dòng thuốc đã đối chiếu")
    @Valid
    private List<BatchRestockItemDto> items;

    @Data
    public static class BatchRestockItemDto {
        @NotNull(message = "Phải chọn mã thuốc trong kho (drugId) cho từng dòng")
        private Long drugId;

        private String ocrDrugName;
        private String batchNumber;
        private LocalDate expiryDate;

        @NotNull(message = "Số lượng nhập không được để trống")
        @Min(value = 1, message = "Số lượng nhập phải tối thiểu là 1")
        private Integer quantityAdded;

        private BigDecimal importUnitPrice;
        private Boolean updateCatalogPrice = false;
    }
}