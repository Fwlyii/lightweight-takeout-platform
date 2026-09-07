package com.tju.elm_bk.service;

import com.tju.elm_bk.constants.ProfileDefaults;
import com.tju.elm_bk.dto.PersonCreateDTO;
import com.tju.elm_bk.entity.Person;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.AuthorityMapper;
import com.tju.elm_bk.mapper.PersonMapper;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.result.ResultCodeEnum;
import com.tju.elm_bk.vo.PersonVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final UserMapper users;
    private final PersonMapper people;
    private final AuthorityMapper authorities;
    private final PasswordEncoder passwords;
    private final ImageStorageService images;

    @Transactional(rollbackFor = Exception.class)
    public PersonVO register(PersonCreateDTO request, MultipartFile avatar) throws IOException {
        try {
            return createAccount(request, avatar);
        } catch (DuplicateKeyException ex) {
            // A concurrent request can win after the checks below. The unique indexes arbitrate;
            // this runtime exception still rolls back all rows and the uploaded file.
            throw new APIException("用户名或手机号已注册");
        }
    }

    private PersonVO createAccount(PersonCreateDTO request, MultipartFile avatar) throws IOException {
        if (users.findByUsername(request.getUsername()) != null) throw new APIException("用户名已存在");
        if (people.countByPhone(request.getPhone()) > 0) throw new APIException("手机号已注册");

        String photo = ProfileDefaults.DEFAULT_AVATAR_URL;
        if (avatar != null && !avatar.isEmpty()) {
            photo = images.storeImage(avatar);
            String stored = photo;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) images.discard(stored);
                }
            });
        }

        var user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwords.encode(request.getPassword()));
        user.setActivated(true);
        user.setIsDeleted(false);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(user.getCreateTime());
        users.insert(user);

        var person = new Person();
        person.setId(user.getId());
        person.setPhone(request.getPhone());
        person.setEmail(request.getEmail());
        person.setFirstName(request.getFirstName());
        person.setLastName(request.getLastName());
        person.setGender(request.getGender());
        person.setPhoto(photo);
        people.insert(person);

        var authority = authorities.findByName("USER");
        if (authority == null) throw new APIException(ResultCodeEnum.SERVER_ERROR.getCode(), "注册暂时不可用");
        users.insertUserAuthority(user.getId(), authority.getName());

        user.setAuthorities(List.of(authority));
        return ProfileViewFactory.from(user, person);
    }
}
