package com.tju.elm_bk.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tju.elm_bk.dto.AiChatRequestDTO;
import com.tju.elm_bk.entity.AiChatHistory;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.AiChatHistoryMapper;
import com.tju.elm_bk.result.ResultCodeEnum;
import com.tju.elm_bk.vo.AiChatHistoryVO;
import com.tju.elm_bk.vo.AiChatResponseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 真实模型与本地帮助共用历史存储、分页和归属校验。 */
@Service
@RequiredArgsConstructor
public class AiChatHistoryService {
    private final AiChatHistoryMapper mapper;
    private final CurrentUserService currentUser;
    private final ObjectMapper json;

    public void requireWritableSession(String sessionId) {
        Long userId = currentUser.requireUserId();
        Long owner = mapper.findUserIdBySessionId(sessionId);
        if (owner != null && !owner.equals(userId)) throw new APIException(ResultCodeEnum.NOT_ENOUGH_PERMISSION);
    }

    public List<AiChatHistory> context(String sessionId) {
        requireWritableSession(sessionId);
        var records = mapper.selectBySessionIdAndUserId(sessionId, currentUser.requireUserId());
        return records.subList(Math.max(0, records.size() - 3), records.size());
    }

    @Transactional
    public void save(AiChatRequestDTO request, AiChatResponseVO response) {
        requireWritableSession(response.getSessionId());
        Long userId = currentUser.requireUserId();
        AiChatHistory record = new AiChatHistory();
        record.setUserId(userId);
        record.setCreator(userId);
        record.setCreateTime(LocalDateTime.now());
        record.setIsDeleted(false);
        record.setSessionId(response.getSessionId());
        record.setUserMessage(request.getMessage());
        record.setAiResponse(response.getMessage());
        record.setChatType(request.getChatType());
        record.setProcessingTime(response.getProcessingTime());
        try {
            record.setContextData(json.writeValueAsString(Map.of("source", response.getSource())));
        } catch (JsonProcessingException ex) {
            throw new APIException(ResultCodeEnum.SERVER_ERROR);
        }
        mapper.insert(record);
    }

    public List<AiChatHistoryVO> list(Long userId, Integer page, Integer size) {
        User actor = currentUser.requireUser();
        Long target = userId == null ? actor.getId() : userId;
        requireReadableUser(actor, target);
        int pageNumber = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 || size > 50 ? 20 : size;
        long offset = (long) (pageNumber - 1) * pageSize;
        if (offset > Integer.MAX_VALUE) throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        return mapper.selectByUserId(target, pageSize, (int) offset).stream().map(this::toView).toList();
    }

    public List<AiChatHistoryVO> session(String sessionId) {
        User actor = currentUser.requireUser();
        Long owner = mapper.findUserIdBySessionId(sessionId);
        if (owner == null) throw new APIException(ResultCodeEnum.NOT_FOUND);
        requireReadableUser(actor, owner);
        return mapper.selectBySessionIdAndUserId(sessionId, owner).stream().map(this::toView).toList();
    }

    @Transactional
    public boolean delete(Long historyId) {
        Long owner = currentUser.requireUserId();
        AiChatHistory record = mapper.selectById(historyId);
        if (record == null) throw new APIException(ResultCodeEnum.NOT_FOUND);
        if (!Objects.equals(record.getUserId(), owner)) throw new APIException(ResultCodeEnum.NOT_ENOUGH_PERMISSION);
        return mapper.deleteById(historyId, owner) > 0;
    }

    @Transactional
    public boolean clean(Integer keepCount) {
        int keep = keepCount == null || keepCount < 10 ? 50 : keepCount;
        return mapper.cleanOldRecords(currentUser.requireUserId(), keep) >= 0;
    }

    private void requireReadableUser(User actor, Long owner) {
        if (!Objects.equals(actor.getId(), owner) && !currentUser.isAdmin(actor)) {
            throw new APIException(ResultCodeEnum.NOT_ENOUGH_PERMISSION);
        }
    }

    private AiChatHistoryVO toView(AiChatHistory record) {
        AiChatHistoryVO result = new AiChatHistoryVO();
        BeanUtils.copyProperties(record, result);
        return result;
    }
}
