package com.tju.elm_bk.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tju.elm_bk.dto.AssistantMessageRequestDTO;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.AiChatHistoryMapper;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.service.AiChatService;
import com.tju.elm_bk.service.AiRecommendationService;
import com.tju.elm_bk.vo.AiRecommendationVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StructuredAssistantServiceImplTest {
    private AiRecommendationService recommendationService;
    private AiChatHistoryMapper historyMapper;
    private StructuredAssistantServiceImpl service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "", List.of()));
        recommendationService = mock(AiRecommendationService.class);
        historyMapper = mock(AiChatHistoryMapper.class);
        when(historyMapper.findUserIdBySessionId(any())).thenReturn(null);
        UserMapper userMapper = mock(UserMapper.class);
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        when(userMapper.findByUsername("alice")).thenReturn(user);
        service = new StructuredAssistantServiceImpl(mock(AiChatService.class), recommendationService,
                historyMapper, userMapper, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnRealCandidatesForRecommendationIntent() {
        AiRecommendationVO candidate = new AiRecommendationVO(1L, "牛肉面", new BigDecimal("25"),
                null, 2L, "面馆", "符合需求");
        when(recommendationService.recommend(any(), any(), org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn(List.of(candidate));
        AssistantMessageRequestDTO request = new AssistantMessageRequestDTO();
        request.setMessage("推荐30元以内的牛肉面");

        var response = service.message(request);

        assertEquals("RECOMMENDATION", response.intent());
        assertEquals(1, response.candidates().size());
        assertTrue(response.needConfirmation());
        verify(historyMapper).insert(any());
    }

    @Test
    void shouldRejectAnotherUsersSession() {
        when(historyMapper.findUserIdBySessionId("other-session")).thenReturn(99L);
        AssistantMessageRequestDTO request = new AssistantMessageRequestDTO();
        request.setMessage("推荐牛肉面");
        request.setSessionId("other-session");

        assertThrows(APIException.class, () -> service.message(request));
    }
}
