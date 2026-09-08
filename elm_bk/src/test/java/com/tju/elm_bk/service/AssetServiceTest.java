package com.tju.elm_bk.service;

import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.entity.UserAsset;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.AssetMapper;
import com.tju.elm_bk.service.impl.AssetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssetServiceTest {
    private final AssetMapper mapper = mock(AssetMapper.class);
    private final CurrentUserService identity = mock(CurrentUserService.class);
    private final AssetServiceImpl service = new AssetServiceImpl(mapper, identity);
    private final UserAsset asset = new UserAsset();

    @BeforeEach
    void setup() {
        User user = new User();
        user.setId(7L);
        when(identity.requireUser()).thenReturn(user);
        asset.setBalance(new BigDecimal("10.00"));
        asset.setPoints(200);
        when(mapper.findByUserId(7L)).thenReturn(asset);
        when(mapper.lockByUserId(7L)).thenReturn(asset);
    }

    @Test
    void couponGrantLocksAccountBeforeCheckingAndWritesLedger() {
        when(mapper.claimWelcomeCoupon(7L)).thenReturn(1);
        service.claimWelcomeCoupon();
        var ordered = inOrder(mapper);
        ordered.verify(mapper).ensure(7L);
        ordered.verify(mapper).lockByUserId(7L);
        ordered.verify(mapper).countWelcomeCoupons(7L);
        ordered.verify(mapper).claimWelcomeCoupon(7L);
        ordered.verify(mapper).insertLedger(7L, "COUPON_GRANT", BigDecimal.ZERO, 0, "领取新人券", null);
    }

    @Test
    void previouslyClaimedCouponCannotBeGrantedAgain() {
        when(mapper.countWelcomeCoupons(7L)).thenReturn(1);
        assertThrows(APIException.class, service::claimWelcomeCoupon);
        verify(mapper, never()).claimWelcomeCoupon(anyLong());
        verify(mapper, never()).insertLedger(any(), any(), any(), any(), any(), any());
    }

    @Test
    void failedCouponWriteCannotProduceSuccessfulLedger() {
        assertThrows(APIException.class, service::claimWelcomeCoupon);
        verify(mapper, never()).insertLedger(any(), any(), any(), any(), any(), any());
    }

    @Test
    void ledgerAndCouponsUseCurrentAccountAndBoundedLimit() {
        service.ledger(null);
        service.ledger(0);
        service.ledger(1000);
        service.availableCoupons();
        verify(mapper).listLedger(7L, 20);
        verify(mapper).listLedger(7L, 1);
        verify(mapper).listLedger(7L, 100);
        verify(mapper).listAvailableCoupons(7L);
    }

    @Test
    void simulatedBenefitsAreDisabledByDefault() {
        assertThrows(APIException.class, () -> service.recharge(BigDecimal.TEN));
        assertThrows(APIException.class, service::activateMembership);
        verifyNoInteractions(mapper);
    }

    @Test
    void rechargeRejectsOutOfRangeAmountAndBalanceOverflow() {
        ReflectionTestUtils.setField(service, "demoEnabled", true);
        assertThrows(APIException.class, () -> service.recharge(null));
        assertThrows(APIException.class, () -> service.recharge(new BigDecimal("0.99")));
        assertThrows(APIException.class, () -> service.recharge(new BigDecimal("501")));
        asset.setBalance(new BigDecimal("1999"));
        assertThrows(APIException.class, () -> service.recharge(BigDecimal.TEN));
        verify(mapper, never()).addBalance(any(), any());
    }

    @Test
    void rechargeRecordsTheSameAmountAsWalletCredit() {
        ReflectionTestUtils.setField(service, "demoEnabled", true);
        BigDecimal amount = new BigDecimal("20.00");
        service.recharge(amount);
        verify(mapper).addBalance(7L, amount);
        verify(mapper).insertLedger(7L, "RECHARGE", amount, 0, "模拟充值", null);
    }

    @Test
    void activeMembershipIsNotExtendedByRepeatedRequests() {
        ReflectionTestUtils.setField(service, "demoEnabled", true);
        asset.setMembershipExpire(LocalDateTime.now().plusDays(5));
        assertTrue(service.activateMembership().isMember());
        verify(mapper, never()).activateMembership(anyLong());
        verify(mapper, never()).insertLedger(any(), any(), any(), any(), any(), any());
    }

    @Test
    void refundPreservesOrderReferenceForBothWalletAndPoints() {
        service.refundOrderAssets(12L, 7L, 200, BigDecimal.TEN);
        verify(mapper).addBalance(7L, BigDecimal.TEN);
        verify(mapper).addPoints(7L, 200);
        verify(mapper).insertLedger(7L, "WALLET_REFUND", BigDecimal.TEN, 0, "订单取消退回钱包余额", 12L);
        verify(mapper).insertLedger(7L, "POINT_REFUND", BigDecimal.ZERO, 200, "订单取消退回抵扣积分", 12L);
    }
}
