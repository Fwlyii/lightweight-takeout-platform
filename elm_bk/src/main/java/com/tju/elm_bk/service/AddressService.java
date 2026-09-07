package com.tju.elm_bk.service;

import com.tju.elm_bk.dto.AddressCreateDTO;
import com.tju.elm_bk.vo.AddressVO;
import java.util.List;

public interface AddressService {
    List<AddressVO> list();
    AddressVO get(Long id);
    AddressVO create(AddressCreateDTO request);
    AddressVO update(Long id, AddressCreateDTO request);
    void delete(Long id);
    AddressVO setDefault(Long id);
}
