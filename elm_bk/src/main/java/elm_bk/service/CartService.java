package elm_bk.service;

import elm_bk.dto.CartItemCreateDTO;
import elm_bk.vo.CartItemVO;
import elm_bk.vo.CartVO;
import io.swagger.v3.oas.models.links.Link;

import java.util.List;

public interface CartService {


    CartVO addCart(CartItemCreateDTO cartItemCreateDTO);



    List<CartItemVO> getCartItemList(Long businessId);

    /** 保存同一商家的备注（口味/餐具等），结算时写入订单。 */
    int updateRemarks(Long businessId, String remarks);

    Long addItem(Long foodId,Integer quantity);

    Long updateItem(Long cartId,Integer quantity);

    Long clearCart(Long businessId);

    Long removeItem(Long cartId);

}
