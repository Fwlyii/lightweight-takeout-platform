package com.tju.elm_bk.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiFoodCandidateVO {
    private Long foodId;
    private String foodName;
    private BigDecimal price;
    private String foodImg;
    private String foodExplain;
    private String category;
    private Long businessId;
    private String businessName;
    private Long salesCount;
    private Long userPurchaseCount;
    private BigDecimal deliveryPrice;
    private BigDecimal startPrice;
    private BigDecimal promotionThreshold;
    private BigDecimal promotionDiscount;
    private BigDecimal businessScore;
    private Integer businessSalesCount;
    private Integer stock;
    private Integer purchaseLimit;
    private Boolean operatingStatus;
}
