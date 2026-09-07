package com.tju.elm_bk.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class MerchantInteractionUpdateDTO {
    @NotNull @Positive
    private Long merchantId;
    private Boolean collected;
    private Boolean liked;
}
