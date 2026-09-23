package elm_bk.service;

import elm_bk.entity.Business;
import elm_bk.exception.APIException;
import elm_bk.mapper.BusinessMapper;
import elm_bk.mapper.FoodMapper;
import elm_bk.mapper.OrdersMapper;
import elm_bk.service.impl.FoodServiceImpl;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class GuestMenuTest {
    @Test
    void guestCannotRequestUnpublishedFoodEvenByPassingStatus() {
        FoodMapper foods = mock(FoodMapper.class);
        BusinessMapper businesses = mock(BusinessMapper.class);
        CurrentUserService users = mock(CurrentUserService.class);
        when(users.optionalUser()).thenReturn(Optional.empty());
        Business business = new Business(); business.setStatus(1);
        when(businesses.selectBusinessById(7L)).thenReturn(business);
        var service = new FoodServiceImpl(foods, businesses, mock(OrdersMapper.class), users);
        service.getFoodItemList(7L, 0);
        verify(foods).selectFoodItemVOList(7L, 1);
    }

    @Test
    void guestCannotReadMenuOfUnapprovedBusiness() {
        FoodMapper foods = mock(FoodMapper.class);
        BusinessMapper businesses = mock(BusinessMapper.class);
        CurrentUserService users = mock(CurrentUserService.class);
        when(users.optionalUser()).thenReturn(Optional.empty());
        Business business = new Business(); business.setStatus(0);
        when(businesses.selectBusinessById(7L)).thenReturn(business);
        var service = new FoodServiceImpl(foods, businesses, mock(OrdersMapper.class), users);
        assertThrows(APIException.class, () -> service.getFoodItemList(7L, null));
        verifyNoInteractions(foods);
    }
}
