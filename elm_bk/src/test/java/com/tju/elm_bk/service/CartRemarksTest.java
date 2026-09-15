package com.tju.elm_bk.service;

import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.entity.Cart;
import com.tju.elm_bk.entity.Food;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.BusinessMapper;
import com.tju.elm_bk.mapper.CartMapper;
import com.tju.elm_bk.mapper.FoodMapper;
import com.tju.elm_bk.service.impl.CartServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CartRemarksTest {
    final CartMapper carts = mock(CartMapper.class);
    final BusinessMapper businesses = mock(BusinessMapper.class);
    final FoodMapper foods = mock(FoodMapper.class);
    final CurrentUserService users = mock(CurrentUserService.class);
    final CartServiceImpl service = new CartServiceImpl(carts, businesses, foods, users, mock(ProductPurchasePolicy.class));

    @Test void trimsRemarksAndAlwaysScopesUpdatesToAuthenticatedOwner() {
        when(users.requireUserId()).thenReturn(11L);
        service.updateRemarks(7L, "  少辣，不要餐具  ");
        verify(carts).updateCartRemarks(11L, 7L, "少辣，不要餐具");
    }

    @Test void validatesMerchantAndRemarkLengthBeforeWriting() {
        assertThrows(APIException.class, () -> service.updateRemarks(0L, "x"));
        when(users.requireUserId()).thenReturn(11L);
        assertThrows(APIException.class, () -> service.updateRemarks(7L, "x".repeat(256)));
        verifyNoInteractions(carts);
    }

    @Test void newFoodInSameMerchantCartInheritsSavedRemark() {
        when(users.requireUserId()).thenReturn(11L);
        Food food = new Food(); food.setId(21L); food.setBusinessId(7L); food.setShelveStatus(1);
        when(foods.selectFoodById(21L)).thenReturn(food);
        Business business = new Business(); business.setStatus(1);
        when(businesses.selectBusinessById(7L)).thenReturn(business);
        when(carts.selectBusinessRemarks(11L, 7L)).thenReturn("不要香菜");
        service.addItem(21L, 1);
        ArgumentCaptor<Cart> row = ArgumentCaptor.forClass(Cart.class);
        verify(carts).insertCart(row.capture());
        assertEquals("不要香菜", row.getValue().getRemarks());
        assertEquals(11L, row.getValue().getCustomerId());
    }
}
