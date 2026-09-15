package com.tju.elm_bk.controller;

import com.tju.elm_bk.dto.CartItemAddDTO;
import com.tju.elm_bk.dto.CartItemCreateDTO;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.service.CartService;
import com.tju.elm_bk.vo.CartItemVO;
import com.tju.elm_bk.vo.CartVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carts")
@Tag(name="管理购物车")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @PostMapping
    @Operation(summary = "向购物车添加商品（旧数据结构兼容接口）", deprecated = true)
    public HttpResult<CartVO> addCartItem(@RequestBody CartItemCreateDTO cartItemCreateDTO) {
        return HttpResult.success(cartService.addCart(cartItemCreateDTO));
    }

    @PostMapping("/items")
    @Operation(summary = "向当前用户购物车添加商品")
    public HttpResult<Long> addItem(@Valid @RequestBody CartItemAddDTO request) {
        return HttpResult.success(cartService.addItem(request.getFoodId(), request.getQuantity()));
    }

    @GetMapping("/list")
    @Operation(summary = "获取当前用户购物车，可按商家筛选")
    public HttpResult<List<CartItemVO>> listItems(@RequestParam(required = false) Long businessId) {
        return HttpResult.success(cartService.getCartItemList(businessId));
    }

    @PutMapping("/{cartId}")
    @Operation(summary = "修改购物车商品数量", description = "quantity传0时移除该条记录")
    public HttpResult<Long> updateItemQuantityRestful(@PathVariable Long cartId, @RequestParam Integer quantity) {
        return HttpResult.success(cartService.updateItem(cartId, quantity));
    }

    @DeleteMapping
    @Operation(summary = "清空用户在指定商家的购物车")
    public HttpResult<Long> clearCart(@RequestParam Long businessId) {
        return HttpResult.success(cartService.clearCart(businessId));
    }

    @DeleteMapping("/{cartId}")
    @Operation(summary = "移除指定购物车商品")
    public HttpResult<Long> removeItemRestful(@PathVariable Long cartId) {
        return HttpResult.success(cartService.removeItem(cartId));
    }


}
