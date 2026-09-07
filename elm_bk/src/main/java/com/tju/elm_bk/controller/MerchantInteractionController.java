package com.tju.elm_bk.controller;

import com.tju.elm_bk.dto.MerchantInteractionUpdateDTO;
import com.tju.elm_bk.entity.MerchantInteraction;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.service.MerchantInteractionService;
import com.tju.elm_bk.vo.BusinessSearchVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/merchant/interaction")
@RequiredArgsConstructor
public class MerchantInteractionController {
    private final MerchantInteractionService interactions;
    @GetMapping("/collections/me") public HttpResult<List<BusinessSearchVO>> collections() {
        return HttpResult.success(interactions.collections());
    }
    @GetMapping("/status/me") public HttpResult<MerchantInteraction> status(@RequestParam Long merchantId) {
        return HttpResult.success(interactions.status(merchantId));
    }
    @PostMapping("/update") public HttpResult<MerchantInteraction> update(@Valid @RequestBody MerchantInteractionUpdateDTO request) {
        return HttpResult.success(interactions.update(request));
    }
}
