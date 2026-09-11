package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.config.DeepSeekConfig;
import com.tju.elm_bk.dto.AiChatRequestDTO;
import com.tju.elm_bk.service.AiChatService;
import com.tju.elm_bk.vo.AiChatHistoryVO;
import com.tju.elm_bk.vo.AiChatResponseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Primary
@Service
@RequiredArgsConstructor
public class RoutingAiChatService implements AiChatService {
    private final DeepSeekConfig config;
    private final AiChatServiceImpl realService;
    private final MockAiChatServiceImpl fallbackService;

    @Override
    public AiChatResponseVO chat(AiChatRequestDTO request) {
        return delegate().chat(request);
    }

    @Override
    public List<AiChatHistoryVO> getChatHistory(Long userId, Integer page, Integer size) {
        return delegate().getChatHistory(userId, page, size);
    }

    @Override
    public List<AiChatHistoryVO> getChatHistoryBySession(String sessionId) {
        return delegate().getChatHistoryBySession(sessionId);
    }

    @Override
    public Boolean deleteChatHistory(Long historyId, Long userId) {
        return delegate().deleteChatHistory(historyId, userId);
    }

    @Override
    public Boolean cleanOldChatHistory(Long userId, Integer keepCount) {
        return delegate().cleanOldChatHistory(userId, keepCount);
    }

    private AiChatService delegate() {
        return config.isConfigured() ? realService : fallbackService;
    }
}
