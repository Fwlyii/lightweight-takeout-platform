package com.tju.elm_bk.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RecommendationRequestDTO {
    @Size(max = 60, message = "推荐关键词不能超过60个字符")
    private String query;

    @DecimalMin(value = "0.01", message = "预算必须大于0")
    @DecimalMax(value = "9999.99", message = "预算不能超过9999.99")
    private BigDecimal budget;

    @Min(value = 1, message = "数量至少为1")
    @Max(value = 99, message = "数量不能超过99")
    private Integer quantity = 1;

    @DecimalMin(value = "0.00", message = "配送费不能小于0")
    @DecimalMax(value = "999.99", message = "配送费不能超过999.99")
    private BigDecimal maxDeliveryFee;

    @DecimalMin(value = "0.00", message = "最低评分不能小于0")
    @DecimalMax(value = "5.00", message = "最低评分不能超过5")
    private BigDecimal minRating;

    private Boolean freeDeliveryOnly = false;

    @Size(max = 30, message = "商品分类不能超过30个字符")
    private String category;

    private Boolean usePreferences = false;
}
