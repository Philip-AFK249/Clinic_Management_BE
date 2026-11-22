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
public class CreateDrugRequest {

    @NotBlank(message = "Mã thuốc không được để trống")
    private String code;

    @NotBlank(message = "Tên thuốc không được để trống")
    private String name;

    @NotBlank(message = "Hàm lượng không được để trống")
    private String concentration;

    @NotBlank(message = "Dạng bào chế không được để trống")
    private String dosageForm;

    @NotBlank(message = "Đơn vị tính không được để trống")
    private String unit;

    @NotNull(message = "Số lượng tồn kho ban đầu không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer stockQuantity;

    @NotNull(message = "Đơn giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Đơn giá phải lớn hơn 0")
    private BigDecimal unitPrice;

    @NotNull(message = "Mức hưởng BHYT không được để trống")
    private BhytCoverage bhytCoverage;

    private Boolean isPenicillinClass = false;
    private LocalDate expiryDate;
}