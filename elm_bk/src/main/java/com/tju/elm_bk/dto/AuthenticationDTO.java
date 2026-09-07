package com.tju.elm_bk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthenticationDTO {
    @NotBlank(message = "请输入用户名或手机号")
    @Size(max = 100)
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(max = 72)
    private String password;

    @NotBlank(message = "请选择登录端")
    @Pattern(regexp = "user|merchant|rider|admin", message = "登录端不合法")
    private String role;

    private Boolean rememberMe = false;
}
