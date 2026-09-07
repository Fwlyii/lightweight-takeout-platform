package com.tju.elm_bk.service;

import com.tju.elm_bk.constants.ProfileDefaults;
import com.tju.elm_bk.entity.Person;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.vo.PersonVO;

/** Explicit response whitelist shared by registration and current-account queries. */
public final class ProfileViewFactory {
    private ProfileViewFactory() {}

    public static PersonVO from(User account, Person profile) {
        var view = new PersonVO();
        view.setId(account.getId());
        view.setUsername(account.getUsername());
        view.setAuthorities(account.getAuthorities());
        view.setActivated(account.getActivated());
        view.setIsDeleted(account.getIsDeleted());
        view.setCreateTime(account.getCreateTime());
        view.setUpdateTime(account.getUpdateTime());
        view.setPhoto(ProfileDefaults.DEFAULT_AVATAR_URL);
        if (profile != null) {
            view.setFirstName(profile.getFirstName());
            view.setLastName(profile.getLastName());
            view.setPhone(profile.getPhone());
            view.setEmail(profile.getEmail());
            view.setGender(profile.getGender());
            if (profile.getPhoto() != null && !profile.getPhoto().isBlank()) view.setPhoto(profile.getPhoto());
        }
        return view;
    }
}
