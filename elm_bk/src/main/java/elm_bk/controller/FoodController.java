package elm_bk.controller;

import elm_bk.dto.FoodCreateDTO;
import elm_bk.dto.FoodDTO;
import elm_bk.dto.FoodUpdateDTO;
import elm_bk.dto.FoodStockUpdateDTO;
import elm_bk.result.HttpResult;
import elm_bk.service.FoodService;
import elm_bk.vo.FoodItemVO;
import elm_bk.vo.FoodVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/foods")
@Tag(name="管理商品")
@RequiredArgsConstructor
public class FoodController {
    private final FoodService foodService;

    @GetMapping
    @Operation(summary = "根据商家或订单获取商品列表",description = "旧数据结构兼容接口", deprecated = true)
    public HttpResult<List<FoodVO>> getAllFoods(@RequestParam(required = false) Integer business, @RequestParam(required = false) Integer order) {
        return HttpResult.success(foodService.getFoodList(business,order));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据商家或订单获取商品列表",description = "旧数据结构兼容接口", deprecated = true)
    public HttpResult<FoodVO> getAllFoods(@PathVariable Long id) {
        return HttpResult.success(foodService.getFoodById(id));
    }

    @PostMapping
    @Operation(summary = "新增商品",description = "旧数据结构兼容接口", deprecated = true)
    @PreAuthorize("hasAnyAuthority('BUSINESS','ADMIN')")
    public HttpResult<FoodVO> addFood(@RequestBody FoodDTO foodDTO) {
        return HttpResult.success(foodService.addFood(foodDTO));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "修改商品信息",description = "旧数据结构兼容接口", deprecated = true)
    @PreAuthorize("hasAnyAuthority('BUSINESS','ADMIN')")
    public HttpResult<FoodVO> modifyFood(@RequestBody FoodDTO foodDTO,@PathVariable Long id) {
        return HttpResult.success(foodService.updateFood(foodDTO,id));
    }

    @GetMapping("/list")
    @Operation(summary = "根据商家获取商品列表",description = "普通用户只能看到已上架的")
    public HttpResult<List<FoodItemVO>> getAllFoods(@RequestParam Long businessId, @RequestParam(required = false) Integer shelveStatus) {
        return HttpResult.success(foodService.getFoodItemList(businessId, shelveStatus));
    }

    @PostMapping("/addItem")
    @Operation(summary = "商铺新增商品",description = "校验当前身份及店铺归属")
    @PreAuthorize("hasAnyAuthority('BUSINESS','ADMIN')")
    public HttpResult<Long> addFoodItem(@RequestBody FoodCreateDTO foodCreateDTO) {
        return HttpResult.success(foodService.addFoodItem(foodCreateDTO));
    }

    @PatchMapping("/{foodId}/status")
    @Operation(summary = "上架/下架商品",description = "shelveStatus 0-下架 1-上架")
    @PreAuthorize("hasAnyAuthority('BUSINESS','ADMIN')")
    public HttpResult<Long> setFoodShelveStatus(@PathVariable Long foodId,@RequestParam Integer shelveStatus) {
        return HttpResult.success(foodService.setFoodStatus(foodId,shelveStatus));
    }

    @PostMapping("/modifyItem")
    @Operation(summary = "商铺修改商品",description = "校验当前身份及店铺归属")
    @PreAuthorize("hasAnyAuthority('BUSINESS','ADMIN')")
    public HttpResult<Long> modifyFoodItem(@RequestBody FoodUpdateDTO foodUpdateDTO) {
        return HttpResult.success(foodService.modifyFoodMessage(foodUpdateDTO));
    }

    @PutMapping("/{id}/stock")
    @Operation(summary = "调整商品库存")
    @PreAuthorize("hasAnyAuthority('BUSINESS','ADMIN')")
    public HttpResult<Long> updateStock(@PathVariable Long id, @RequestBody FoodStockUpdateDTO dto) {
        if (dto == null || dto.getStock() == null || dto.getStock() < 0 || dto.getStock() > 1_000_000) {
            throw new elm_bk.exception.APIException("库存必须是0到1000000之间的整数");
        }
        FoodUpdateDTO update = new FoodUpdateDTO();
        update.setFoodId(id); update.setStock(dto.getStock());
        // 复用商品服务的归属校验，库存修改不允许越权。
        return HttpResult.success(foodService.updateStock(update));
    }

    @DeleteMapping("/{foodId}")
    @Operation(summary = "商家删除商品")
    @PreAuthorize("hasAnyAuthority('BUSINESS','ADMIN')")
    public HttpResult<Long> deleteFood(@PathVariable Long foodId) {
        return HttpResult.success(foodService.deleteFood(foodId));
    }

}
