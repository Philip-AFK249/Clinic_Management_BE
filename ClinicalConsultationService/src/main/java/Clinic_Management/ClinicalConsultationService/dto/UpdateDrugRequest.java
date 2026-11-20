package Clinic_Management.ClinicalConsultationService.dto;
import Clinic_Management.ClinicalConsultationService.entity.BhytCoverage;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateDrugRequest {

    @NotBlank(message = "Tên thuốc không được để trống")
    private String name;

    @NotBlank(message = "Hàm lượng không được để trống")
    private String concentration;

    @NotBlank(message = "Dạng bào chế không được để trống")
    private String dosageForm;

    @NotNull(message = "Đơn giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Đơn giá phải lớn hơn 0")
    private BigDecimal unitPrice;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn không được âm")
    private Integer stockQuantity;

    @NotNull(message = "Mức hưởng BHYT không được để trống")
    private BhytCoverage bhytCoverage;

    private Boolean isPenicillinClass = false;
    private Boolean active = true;
    private LocalDate expiryDate;
}