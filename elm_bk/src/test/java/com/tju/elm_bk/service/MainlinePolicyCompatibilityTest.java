package com.tju.elm_bk.service;

import com.tju.elm_bk.constant.AuthorityName;
import com.tju.elm_bk.constant.OrderStatus;
import com.tju.elm_bk.entity.Authority;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.vo.BusinessSearchVO;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MainlinePolicyCompatibilityTest {
    @Test
    void adminCheckDoesNotGrantPermissionToMissingOrOrdinaryAccount() {
        var service = new CurrentUserService(null);
        assertFalse(service.isAdmin(null));
        User user = new User();
        assertFalse(service.isAdmin(user));
        Authority authority = new Authority();
        authority.setName("USER");
        user.setAuthorities(List.of(authority));
        assertFalse(service.isAdmin(user));
        authority.setName("ADMIN");
        assertTrue(service.isAdmin(user));
        assertTrue(AuthorityName.ADMIN.isGrantedTo(user));
    }

    @Test
    void invalidOrderStateProducesBusinessErrorInsteadOfNullPointer() {
        assertEquals(OrderStatus.CANCELLED, OrderStatus.fromCode(8));
        assertThrows(APIException.class, () -> OrderStatus.fromCode(null));
        assertThrows(APIException.class, () -> OrderStatus.fromCode(999));
    }

    @Test
    void recommendationPolicyHonorsQualificationAndVisibleTagLimit() {
        var policy = new BusinessRecommendationPolicy(new BusinessPricingPolicy());
        var business = new BusinessSearchVO();
        business.setScore(new BigDecimal("4.9"));
        business.setSalesCount(199);
        policy.enrich(business, false);
        assertFalse(business.getRecommendationTags().contains("好评如潮"));
        business.setSalesCount(200);
        policy.enrich(business, false);
        assertTrue(business.getRecommendationTags().contains("好评如潮"));
        business.setPromotionThreshold(new BigDecimal("30"));
        business.setPromotionDiscount(new BigDecimal("5"));
        policy.enrich(business, true);
        assertEquals("上次买过", business.getRecommendationTags().get(0));
        assertEquals(3, business.getRecommendationTags().size());
    }
}
