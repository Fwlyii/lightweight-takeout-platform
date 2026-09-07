package com.tju.elm_bk.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/** Editable address fields only. Ownership, default selection and audit fields are server-owned. */
@Data
public class AddressCreateDTO {
    @NotBlank @Size(max = 40)
    private String contactName;
    @Min(0) @Max(1)
    private Integer contactSex;
    @NotBlank @Pattern(regexp = "^1[3-9][0-9]{9}$")
    private String contactTel;
    @NotBlank @Size(max = 255)
    private String address;
}
