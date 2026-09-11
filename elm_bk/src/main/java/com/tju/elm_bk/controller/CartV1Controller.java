package com.tju.elm_bk.controller;

import com.tju.elm_bk.dto.CartItemAddDTO;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart/items")
@RequiredArgsConstructor
public class CartV1Controller {
    private final CartService cartService;

    @PostMapping
    public HttpResult<Long> add(@Valid @RequestBody CartItemAddDTO request) {
        return HttpResult.success(cartService.addItem(request.getFoodId(), request.getQuantity()));
    }
}
