// OrderDetailetMapper.java
package com.tju.elm_bk.mapper;

import com.tju.elm_bk.entity.OrderDetailet;

import com.tju.elm_bk.vo.OrderFoodVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderDetailetMapper {

    Integer saveOrderDetailPlus(OrderDetailet orderDetailet);

    @Select("""
        select od.id,od.quantity,od.food_id,
           COALESCE(od.food_name_snapshot, '历史商品（名称未留存）') as food_name,od.food_price,
           od.order_id
        from orderdetailet od
        where od.order_id = #{orderId}
    """)
    List<OrderFoodVO> selectOrderDetailList(Long orderId);
}
