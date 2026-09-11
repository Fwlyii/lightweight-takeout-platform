package com.tju.elm_bk.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AssistantMessageRequestDTO {
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 500, message = "消息内容不能超过500个字符")
    private String message;

    @Size(max = 64, message = "会话ID不能超过64个字符")
    private String sessionId;

    @DecimalMin(value = "0.01", message = "预算必须大于0")
    @DecimalMax(value = "9999.99", message = "预算不能超过9999.99")
    private BigDecimal budget;

    private Boolean usePreferences = true;
}
