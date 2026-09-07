package com.tju.elm_bk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Self-registration accepts profile fields only; authority and account status are server-owned. */
@Data
public class PersonCreateDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 20, message = "用户名不能超过20个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度需为8–32位")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码须同时包含字母和数字")
    private String password;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入11位有效手机号")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 255)
    private String email;
    @Size(max = 255) private String firstName;
    @Size(max = 255) private String lastName;
    @Size(max = 20) private String gender;

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }
}
