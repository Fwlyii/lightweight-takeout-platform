package elm_bk.vo;

import java.math.BigDecimal;
import elm_bk.exception.APIException;
import elm_bk.result.ResultCodeEnum;

/** Storefront fields only: no owner account or audit identifiers. */
public record PublicBusinessVO(Long id, String businessName, String businessAddress,
        String businessExplain, String businessImg, Integer orderTypeId,
        BigDecimal startPrice, BigDecimal deliveryPrice, String remarks,
        Boolean dineInAvailable, Integer status, Boolean operatingStatus,
        BigDecimal promotionThreshold, BigDecimal promotionDiscount,
        BigDecimal longitude, BigDecimal latitude, String poiId, String adcode, String formattedAddress) {
    public static PublicBusinessVO from(BusinessVO business) {
        if (business == null || !Integer.valueOf(1).equals(business.getStatus())) {
            throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        }
        return new PublicBusinessVO(business.getId(), business.getBusinessName(), business.getBusinessAddress(),
                business.getBusinessExplain(), business.getBusinessImg(), business.getOrderTypeId(),
                business.getStartPrice(), business.getDeliveryPrice(), business.getRemarks(),
                business.getDineInAvailable(), business.getStatus(), business.getOperatingStatus(),
                business.getPromotionThreshold(), business.getPromotionDiscount(), business.getLongitude(), business.getLatitude(),
                business.getPoiId(), business.getAdcode(), business.getFormattedAddress());
    }
}
