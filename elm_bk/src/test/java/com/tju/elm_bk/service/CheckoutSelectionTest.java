package com.tju.elm_bk.service;

import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.*;
import com.tju.elm_bk.vo.CartItemVO;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CheckoutSelectionTest {
    @Test void selectedItemsCannotSilentlyDisappearDuringCheckout() {
        OrdersMapper orders = mock(OrdersMapper.class);
        BusinessMapper businesses = mock(BusinessMapper.class);
        CartMapper carts = mock(CartMapper.class);
        FoodMapper foods = mock(FoodMapper.class);
        CurrentUserService users = mock(CurrentUserService.class);
        Business business = new Business(); business.setStatus(1); business.setDineInAvailable(true);
        when(businesses.lockBusinessById(1L)).thenReturn(business);
        when(users.requireUserId()).thenReturn(11L);
        CartItemVO item = new CartItemVO(); item.setFoodId(21L);
        when(carts.selectCartItems(11L, 1L)).thenReturn(List.of(item));
        var service = new OrderSubmissionService(orders, businesses, mock(DeliveryAddressMapper.class), carts,
                foods, mock(OrderDetailetMapper.class), mock(AssetMapper.class), users,
                mock(OrderPricingService.class), mock(OrderStateTransitionService.class), new ProductPurchasePolicy());
        assertThrows(APIException.class, () -> service.submit(1L, null, null, "pickup", List.of(21L, 22L)));
        verifyNoInteractions(orders, foods);
    }
}
