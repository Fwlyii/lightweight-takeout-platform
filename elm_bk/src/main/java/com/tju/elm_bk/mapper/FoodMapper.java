package com.tju.elm_bk.mapper;
import java.util.List;

import com.tju.elm_bk.dto.FoodCreateDTO;
import com.tju.elm_bk.entity.Food;
import com.tju.elm_bk.vo.AiFoodCandidateVO;
import com.tju.elm_bk.vo.FoodItemVO;
import com.tju.elm_bk.vo.FoodVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FoodMapper {


    List<FoodVO> selectFoodVOList(@Param("businessId") Integer businessId, @Param("orderId") Integer orderId,
                                  @Param("visibleOnly") boolean visibleOnly);

    FoodVO selectFoodVOById(@Param("id") Long id);


    void insertFood(Food food);

    @Select("SELECT * FROM food WHERE is_deleted = 0 AND id = #{id}")
    Food selectFoodById(@Param("id") Long id);

    @Update("update food set update_time = #{food.updateTime}, updater = #{food.updater}, food_explain = #{food.foodExplain}, food_img =#{food.foodImg}, food_name = #{food.foodName}, food_price = #{food.foodPrice}, remarks = #{food.remarks}, category = #{food.category}, purchase_limit = #{food.purchaseLimit} where id = #{foodId}")
    void updateFood(@Param("food") Food food, @Param("foodId") Long foodId);



    List<FoodItemVO> selectFoodItemVOList(@Param("businessId") Long businessId, @Param("shelveStatus") Integer shelveStatus);

    @Update("update food set shelve_status = #{shelveStatus} where id = #{foodId}")
    void updateFoodStatus(@Param("foodId") Long foodId, @Param("shelveStatus") Integer shelveStatus);

    /** 原子扣减库存，只有上架且库存足够时才会成功。 */
    @Update("UPDATE food SET stock = stock - #{quantity}, update_time = NOW() WHERE id = #{foodId} AND is_deleted = 0 AND shelve_status = 1 AND stock >= #{quantity}")
    int decrementStock(@Param("foodId") Long foodId, @Param("quantity") Integer quantity);

    /** 订单取消/超时后一次性恢复该订单预占的库存。 */
    @Update("UPDATE food f JOIN (SELECT food_id, SUM(quantity) quantity FROM orderdetailet WHERE order_id = #{orderId} AND is_deleted = 0 GROUP BY food_id) d ON d.food_id = f.id SET f.stock = f.stock + d.quantity, f.update_time = NOW()")
    int restoreStockByOrder(@Param("orderId") Long orderId);

    @Update("update food set update_time = #{updateTime}, updater = #{updater}, food_explain = #{foodExplain}, food_img = #{foodImg}, food_name = #{foodName}, food_price = #{foodPrice}, remarks = #{remarks}, stock = #{stock}, category = #{category}, purchase_limit = #{purchaseLimit} where id = #{id}")
    void updateFoodMessage(Food food);

    @Update("update food set is_deleted = 1 where id = #{foodId}")
    void deleteFood(@Param("foodId") Long foodId);

    // AI服务相关查询方法
    @Select("<script>" +
            "SELECT f.* FROM food f JOIN business b ON b.id = f.business_id AND b.is_deleted = 0 AND b.status = 1 " +
            "WHERE f.is_deleted = 0 AND f.shelve_status = 1 AND f.stock > 0 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "   AND (f.food_name LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR f.food_explain LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "ORDER BY f.create_time DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<Food> searchByKeyword(@Param("keyword") String keyword, @Param("limit") Integer limit);


    @Select("""
            SELECT f.id AS food_id, f.food_name, f.food_price AS price,
                   COALESCE(NULLIF(f.food_img, ''),
                     CASE
                       WHEN f.food_name LIKE '%面%' THEN '/images/foods/04-noodles.jpg'
                       WHEN f.food_name LIKE '%麻辣烫%' THEN '/images/foods/24-malatang.jpg'
                       WHEN f.food_name LIKE '%火锅%' THEN '/images/foods/17-hot-pot.jpg'
                       WHEN f.food_name LIKE '%饺%' THEN '/images/foods/22-jiaozi.jpg'
                       WHEN f.food_name LIKE '%包%' THEN '/images/foods/20-baozi.jpg'
                       WHEN f.food_name LIKE '%汉堡%' THEN '/images/foods/12-burger.jpg'
                       WHEN f.food_name LIKE '%咖啡%' THEN '/images/foods/08-coffee.jpg'
                       WHEN f.food_name LIKE '%茶%' OR f.food_name LIKE '%饮%' THEN '/images/foods/15-tea.jpg'
                       WHEN f.food_name LIKE '%蛋糕%' OR f.food_name LIKE '%甜品%' THEN '/images/foods/10-cheesecake.jpg'
                       WHEN f.food_name LIKE '%沙拉%' OR f.category LIKE '%轻食%' THEN '/images/foods/01-chicken-salad.jpg'
                       WHEN f.food_name LIKE '%饭%' OR f.category LIKE '%盖饭%' THEN '/images/foods/26-teriyaki-rice.jpg'
                       ELSE '/images/foods/03-stir-fry.jpg'
                     END) AS food_img,
                   f.food_explain, f.category, f.business_id, b.business_name,
                   b.delivery_price, b.start_price, b.promotion_threshold, b.promotion_discount,
                   b.operating_status, f.stock, f.purchase_limit,
                   COALESCE((SELECT ROUND(AVG(r.rating), 2) FROM review r
                              WHERE r.business_id = b.id AND r.is_hidden = 0), b.demo_rating, 0) AS business_score,
                   (COALESCE(b.demo_sales_count, 0) +
                     (SELECT COUNT(*) FROM orders completed_order
                       WHERE completed_order.business_id = b.id AND completed_order.order_state = 7
                         AND completed_order.is_deleted = 0)) AS business_sales_count,
                   COALESCE(SUM(CASE WHEN o.id IS NOT NULL THEN od.quantity ELSE 0 END), 0) AS sales_count,
                   COALESCE(SUM(CASE WHEN o.customer_id = #{userId} THEN od.quantity ELSE 0 END), 0) AS user_purchase_count
            FROM food f
            JOIN business b ON b.id = f.business_id AND b.is_deleted = 0 AND b.status = 1
            LEFT JOIN orderdetailet od ON od.food_id = f.id AND od.is_deleted = 0
            LEFT JOIN orders o ON o.id = od.order_id AND o.is_deleted = 0 AND o.order_state = 7
            WHERE f.is_deleted = 0 AND f.shelve_status = 1 AND f.stock > 0
            GROUP BY f.id, f.food_name, f.food_price, f.food_img, f.food_explain, f.category,
                     f.business_id, b.id, b.business_name, b.delivery_price, b.start_price,
                     b.promotion_threshold, b.promotion_discount, b.operating_status, b.demo_rating,
                     b.demo_sales_count, f.stock, f.purchase_limit, f.create_time
            ORDER BY sales_count DESC, f.create_time DESC
            LIMIT #{limit}
            """)
    List<AiFoodCandidateVO> listAiCandidates(@Param("userId") Long userId, @Param("limit") Integer limit);

}
