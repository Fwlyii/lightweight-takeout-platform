package com.tju.elm_bk.controller;

import com.tju.elm_bk.dto.PersonCreateDTO;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.service.RegistrationService;
import com.tju.elm_bk.vo.PersonVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserRestController {
    private final RegistrationService registration;

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public HttpResult<PersonVO> register(@Valid @RequestBody PersonCreateDTO request) throws IOException {
        return HttpResult.success(registration.register(request, null));
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public HttpResult<PersonVO> registerWithAvatar(@Valid @RequestPart("user") PersonCreateDTO request,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar) throws IOException {
        return HttpResult.success(registration.register(request, avatar));
    }
}
