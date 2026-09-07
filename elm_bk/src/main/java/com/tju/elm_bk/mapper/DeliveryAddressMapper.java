package com.tju.elm_bk.mapper;

import com.tju.elm_bk.entity.DeliveryAddress;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DeliveryAddressMapper {
    @Select("SELECT * FROM delivery_address WHERE id=#{id} AND user_id=#{userId} AND is_deleted=0")
    DeliveryAddress findOwned(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE delivery_address SET contact_name=#{a.contactName},contact_sex=#{a.contactSex},contact_tel=#{a.contactTel},address=#{a.address},updater=#{userId},update_time=NOW() WHERE id=#{id} AND user_id=#{userId} AND is_deleted=0")
    int updateOwned(@Param("id") Long id, @Param("userId") Long userId, @Param("a") com.tju.elm_bk.dto.AddressCreateDTO address);

    @Update("UPDATE delivery_address SET is_deleted=1,is_default=0,updater=#{userId},update_time=NOW() WHERE id=#{id} AND user_id=#{userId} AND is_deleted=0")
    int deleteOwned(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE delivery_address SET is_default=CASE WHEN id=#{id} THEN 1 ELSE 0 END,updater=#{userId},update_time=NOW() WHERE user_id=#{userId} AND is_deleted=0")
    int selectDefault(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT * FROM delivery_address WHERE id=#{id}")
    public DeliveryAddress getDeliveryAddressById(Long id);

    public int updateDeliveryAddress(DeliveryAddress deliveryAddress);

    void insert(DeliveryAddress address);

    @Select("SELECT * FROM delivery_address WHERE user_id=#{userId} AND is_deleted=0 ORDER BY is_default DESC,id ASC")
    List<DeliveryAddress> listDeliveryAddressByUserId(Long userId);
}
