package com.tju.elm_bk.controller;

import com.tju.elm_bk.dto.PreferenceUpdateDTO;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.entity.UserPreference;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.PreferenceMapper;
import com.tju.elm_bk.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PreferenceControllerTest {
    private PreferenceMapper preferenceMapper;
    private PreferenceController controller;

    @BeforeEach
    void setUp() {
        preferenceMapper = mock(PreferenceMapper.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        User user = new User();
        user.setId(7L);
        when(currentUserService.requireUser()).thenReturn(user);
        controller = new PreferenceController(preferenceMapper, currentUserService);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"light", "dark", "mint", "warm"})
    void acceptsEveryThemeOfferedByTheFrontend(String theme) {
        PreferenceUpdateDTO request = preference(theme);
        UserPreference saved = new UserPreference();
        saved.setUserId(7L);
        saved.setTheme(theme);
        saved.setSpicyLevel(0);
        when(preferenceMapper.findByUserId(7L)).thenReturn(saved);

        controller.update(request);

        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(preferenceMapper).upsert(captor.capture());
        assertEquals(theme, captor.getValue().getTheme());
        assertEquals(7L, captor.getValue().getUserId());
    }

    @Test
    void rejectsUnknownThemeInsteadOfSilentlyPersistingIt() {
        assertThrows(APIException.class, () -> controller.update(preference("neon-rainbow")));
        verify(preferenceMapper, org.mockito.Mockito.never()).upsert(any());
    }

    private PreferenceUpdateDTO preference(String theme) {
        PreferenceUpdateDTO request = new PreferenceUpdateDTO();
        request.setTheme(theme);
        request.setSpicyLevel(0);
        return request;
    }

    @Test
    void missingPreferenceReturnsDefaultsAndClearUsesCurrentAccount() {
        assertEquals("light", controller.me().getData().getTheme());
        assertEquals(0, controller.me().getData().getSpicyLevel());
        controller.clear();
        verify(preferenceMapper).deleteByUserId(7L);
    }

    @Test
    void rejectsMissingPayloadInvalidSpiceAndOversizedTags() {
        assertThrows(APIException.class, () -> controller.update(null));
        PreferenceUpdateDTO dto = preference("light");
        dto.setSpicyLevel(4);
        assertThrows(APIException.class, () -> controller.update(dto));
        dto.setSpicyLevel(-1);
        assertThrows(APIException.class, () -> controller.update(dto));
        dto.setSpicyLevel(0);
        dto.setTasteTags("a".repeat(201));
        assertThrows(APIException.class, () -> controller.update(dto));
        verify(preferenceMapper, org.mockito.Mockito.never()).upsert(any());
    }
}
