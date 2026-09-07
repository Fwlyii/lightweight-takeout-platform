package com.tju.elm_bk.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/** This endpoint cannot modify login identity, grants, password or arbitrary image URLs. */
@Data
public class ProfileUpdateDTO {
    @NotBlank @Pattern(regexp = "^1[3-9][0-9]{9}$")
    private String phone;
    @Email @Size(max = 255)
    private String email;
    @Size(max = 40)
    private String firstName;
    @Size(max = 40)
    private String lastName;
    @Size(max = 20)
    private String gender;
}
