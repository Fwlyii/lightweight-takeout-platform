package com.tju.elm_bk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MerchantInteractionDTO {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "由服务端从登录态注入的用户 ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long userId;

    @NotNull(message = "商铺ID不能为空")
    @Schema(description = "商铺ID")
    private Long merchantId;

    // 两个状态允许单独修改；没有传入的字段保持原值。
    private Boolean liked;
    private Boolean collected;
}
