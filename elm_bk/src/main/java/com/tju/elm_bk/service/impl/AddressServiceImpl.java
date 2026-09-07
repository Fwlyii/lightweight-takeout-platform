package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.dto.AddressCreateDTO;
import com.tju.elm_bk.entity.DeliveryAddress;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.DeliveryAddressMapper;
import com.tju.elm_bk.result.ResultCodeEnum;
import com.tju.elm_bk.service.*;
import com.tju.elm_bk.vo.AddressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER')")
public class AddressServiceImpl implements AddressService {
    private final DeliveryAddressMapper addresses;
    private final CurrentUserService currentUser;
    private final AccountWriteLock writeLock;

    public List<AddressVO> list() {
        return addresses.listDeliveryAddressByUserId(currentUser.requireUserId()).stream().map(this::view).toList();
    }
    public AddressVO get(Long id) { return view(owned(id, currentUser.requireUserId())); }

    @Transactional
    public AddressVO create(AddressCreateDTO request) {
        Long userId = writeLock.acquire();
        var address = new DeliveryAddress();
        address.setUserId(userId);
        address.setContactName(request.getContactName().trim());
        address.setContactSex(request.getContactSex());
        address.setContactTel(request.getContactTel());
        address.setAddress(request.getAddress().trim());
        address.setCreator(userId);
        address.setUpdater(userId);
        address.setCreateTime(LocalDateTime.now());
        address.setUpdateTime(address.getCreateTime());
        address.setIsDeleted(false);
        address.setIsDefault(addresses.listDeliveryAddressByUserId(userId).isEmpty());
        addresses.insert(address);
        return view(address);
    }

    @Transactional
    public AddressVO update(Long id, AddressCreateDTO request) {
        Long userId = writeLock.acquire();
        owned(id, userId);
        request.setContactName(request.getContactName().trim());
        request.setAddress(request.getAddress().trim());
        addresses.updateOwned(id, userId, request);
        return view(owned(id, userId));
    }

    @Transactional
    public void delete(Long id) {
        Long userId = writeLock.acquire();
        var previous = owned(id, userId);
        addresses.deleteOwned(id, userId);
        if (Boolean.TRUE.equals(previous.getIsDefault())) {
            var remaining = addresses.listDeliveryAddressByUserId(userId);
            if (!remaining.isEmpty()) addresses.selectDefault(remaining.get(0).getId(), userId);
        }
    }

    @Transactional
    public AddressVO setDefault(Long id) {
        Long userId = writeLock.acquire();
        owned(id, userId);
        addresses.selectDefault(id, userId);
        return view(owned(id, userId));
    }

    private DeliveryAddress owned(Long id, Long userId) {
        var address = addresses.findOwned(id, userId);
        if (address == null) throw new APIException(ResultCodeEnum.ADDRESS_MISSED);
        return address;
    }
    private AddressVO view(DeliveryAddress address) {
        var view = new AddressVO();
        view.setId(address.getId());
        view.setContactName(address.getContactName());
        view.setContactSex(address.getContactSex());
        view.setContactTel(address.getContactTel());
        view.setAddress(address.getAddress());
        view.setIsDefault(Boolean.TRUE.equals(address.getIsDefault()));
        return view;
    }
}
