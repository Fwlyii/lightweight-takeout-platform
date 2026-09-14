package com.tju.elm_bk.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class AiRecommendationVO {
    public AiRecommendationVO(Long foodId, String foodName, BigDecimal price, String foodImg,
                              Long businessId, String businessName, String reason) {
        this.foodId = foodId;
        this.foodName = foodName;
        this.price = price;
        this.foodImg = foodImg;
        this.businessId = businessId;
        this.businessName = businessName;
        this.reason = reason;
    }
    private Long foodId;
    private String foodName;
    private BigDecimal price;
    private String foodImg;
    private Long businessId;
    private String businessName;
    private String reason;
    private Integer quantity;
    private BigDecimal subtotal;
    private BigDecimal deliveryPrice;
    private BigDecimal startPrice;
    private BigDecimal promotionThreshold;
    private BigDecimal promotionDiscount;
    private BigDecimal estimatedTotal;
    private BigDecimal amountToStartPrice;
    private BigDecimal businessScore;
    private Integer businessSalesCount;
    private Integer stock;
    private Integer purchaseLimit;
    private String category;
    private Boolean operatingStatus;
}
