package elm_bk.service;

import elm_bk.vo.AssetVO;
import elm_bk.vo.AssetLedgerVO;
import elm_bk.entity.UserCoupon;
import java.util.List;

import java.math.BigDecimal;

public interface AssetService {
    AssetVO me();
    AssetVO recharge(BigDecimal amount);
    AssetVO claimWelcomeCoupon();
    List<UserCoupon> availableCoupons();
    AssetVO activateMembership();
    List<AssetLedgerVO> ledger(Integer limit);

    void refundOrderAssets(Long orderId, Long userId, Integer pointsUsed, java.math.BigDecimal walletAmount);
}
