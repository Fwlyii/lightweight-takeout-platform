package com.tju.elm_bk.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tju.elm_bk.config.DeepSeekConfig;
import com.tju.elm_bk.dto.DeepSeekRequestDTO;
import com.tju.elm_bk.dto.DeepSeekResponseDTO;
import com.tju.elm_bk.utils.DeepSeekApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 仅替代外部模型请求；接口、身份校验、服务、Mapper 和数据库都实际执行。 */
@SpringBootTest(properties = {
    "spring.datasource.url=${ai.test.jdbc-url:jdbc:h2:mem:ai_history;MODE=MySQL;DB_CLOSE_DELAY=-1}",
    "spring.datasource.driver-class-name=${ai.test.jdbc-driver:org.h2.Driver}",
    "spring.datasource.username=${ai.test.jdbc-user:sa}",
    "spring.datasource.password=${ai.test.jdbc-password:}",
    "spring.sql.init.mode=${ai.test.init-mode:always}",
    "spring.sql.init.schema-locations=classpath:auth-schema.sql,classpath:order-snapshot-schema.sql,classpath:ai-history-schema.sql"
})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("auth-test")
class AiChatJourneyTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired DeepSeekConfig config;
    @MockBean DeepSeekApiClient provider;

    @BeforeEach
    void prepareIsolatedAccounts() throws Exception {
        try (var connection = jdbc.getDataSource().getConnection()) {
            assertTrue("AI_HISTORY".equalsIgnoreCase(connection.getCatalog())
                    || "ai_history_test".equals(connection.getCatalog()), "仅允许专用测试数据库");
        }
        for (String table : new String[]{"ai_chat_history", "notification", "user_asset_ledger", "user_coupon",
                "order_status_history", "orderdetailet", "orders", "cart", "user_asset", "food", "business",
                "delivery_address", "user_authority", "person", "users", "authority"}) jdbc.update("DELETE FROM " + table);
        jdbc.update("INSERT INTO authority(name) VALUES ('USER')");
        jdbc.update("INSERT INTO users(id,username,password,activated,is_deleted) VALUES "
                + "(1,'owner','test-only',1,0),(2,'other','test-only',1,0)");
        jdbc.update("INSERT INTO user_authority(user_id,authority_name) VALUES (1,'USER'),(2,'USER')");
        config.setApiKey("");
        config.setModel("test-model");
        config.setMaxTokens(256);
        config.setTemperature(0.2);
        config.setTopP(0.8);
        when(provider.chatCompletionSync(any())).thenReturn(modelReply("这是外部模型的测试回复"));
    }

    @Test void configuredProviderIsUsedAndItsResponseIsPersistedOnce() throws Exception {
        config.setApiKey("test-only-key");
        JsonNode data = data(chat(Map.of("message", "你好", "userId", 2)));
        assertEquals("这是外部模型的测试回复", data.path("message").asText());
        assertEquals("deepseek", data.path("source").asText());
        assertFalse(data.path("sessionId").asText().isBlank());
        assertEquals(1, count());
        assertEquals(1L, jdbc.queryForObject("SELECT user_id FROM ai_chat_history", Long.class));
        var argument = ArgumentCaptor.forClass(DeepSeekRequestDTO.class);
        verify(provider).chatCompletionSync(argument.capture());
        assertEquals("test-model", argument.getValue().getModel());
        assertEquals(256, argument.getValue().getMaxTokens());
        assertEquals(0.2, argument.getValue().getTemperature());
        assertEquals(0.8, argument.getValue().getTopP());
    }

    @Test void missingCredentialsUseLocalHelpWithoutCallingTheProvider() throws Exception {
        JsonNode response = data(chat(Map.of("message", "你好")));
        assertEquals("local", response.path("source").asText());
        assertFalse(response.path("message").asText().isBlank());
        assertEquals(1, count());
        verifyNoInteractions(provider);
    }

    @Test void providerFailureFallsBackWithoutLosingTheSessionOrDuplicatingHistory() throws Exception {
        config.setApiKey("test-only-key");
        when(provider.chatCompletionSync(any())).thenThrow(new IllegalStateException("provider unavailable"));
        JsonNode response = data(chat(Map.of("message", "你好", "sessionId", "existing-session")));
        assertEquals("local", response.path("source").asText());
        assertEquals("existing-session", response.path("sessionId").asText());
        assertEquals("text", response.path("responseType").asText());
        assertEquals(1, count());
        verify(provider).chatCompletionSync(any());
    }

    @Test void emptyProviderReplyAlsoUsesLocalHelp() throws Exception {
        config.setApiKey("test-only-key");
        when(provider.chatCompletionSync(any())).thenReturn(new DeepSeekResponseDTO());
        assertEquals("local", data(chat(Map.of("message", "你好"))).path("source").asText());
        assertEquals(1, count());
    }

    @Test void anotherUsersHistoryCannotBeDeleted() throws Exception {
        seed(10, 2, "other-session", "private message");
        mvc.perform(delete("/api/ai/chat/history/10").with(user("owner").authorities(() -> "USER")))
                .andExpect(status().isForbidden());
        assertEquals(0, jdbc.queryForObject("SELECT is_deleted FROM ai_chat_history WHERE id=10", Integer.class));
    }

    @Test void ownHistoryCanBeDeletedButNotDeletedTwice() throws Exception {
        seed(10, 1, "own-session", "own message");
        mvc.perform(delete("/api/ai/chat/history/10").with(user("owner").authorities(() -> "USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));
        assertEquals(1, jdbc.queryForObject("SELECT is_deleted FROM ai_chat_history WHERE id=10", Integer.class));
        mvc.perform(delete("/api/ai/chat/history/10").with(user("owner").authorities(() -> "USER")))
                .andExpect(status().isNotFound());
    }

    @Test void anotherUsersSessionCannotBeReadOrContinued() throws Exception {
        seed(10, 2, "other-session", "private message");
        mvc.perform(get("/api/ai/chat/history/session/other-session").with(user("owner").authorities(() -> "USER")))
                .andExpect(status().isForbidden());
        chat(Map.of("message", "继续", "sessionId", "other-session")).andExpect(status().isForbidden());
        mvc.perform(get("/api/ai/chat/history").param("userId", "2").with(user("owner").authorities(() -> "USER")))
                .andExpect(status().isForbidden());
        assertEquals(1, count());
        verifyNoInteractions(provider);
    }

    @Test void anonymousRequestsCannotCreateHistory() throws Exception {
        mvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"你好\"}"))
                .andExpect(status().isUnauthorized());
        assertEquals(0, count());
    }

    @ParameterizedTest @ValueSource(strings = {"", "   ", "LONG_MESSAGE"})
    void invalidMessagesAreRejectedBeforeProviderCalls(String message) throws Exception {
        chat(Map.of("message", "LONG_MESSAGE".equals(message) ? "问".repeat(1001) : message))
                .andExpect(status().isBadRequest());
        assertEquals(0, count());
        verifyNoInteractions(provider);
    }

    @Test void historyOnlyIncludesTheRequestedUsersVisibleRecords() throws Exception {
        seed(10, 1, "own-session", "own message");
        seed(11, 2, "other-session", "private message");
        mvc.perform(get("/api/ai/chat/history").with(user("owner").authorities(() -> "USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].userMessage").value("own message"));
    }

    @Test void providerContextContainsOnlyTheLastThreeTurnsOfTheOwnedSession() throws Exception {
        config.setApiKey("test-only-key");
        for (int i = 1; i <= 5; i++) seed(i, 1, "own-session", "turn-" + i);
        seed(20, 2, "other-session", "private message");
        data(chat(Map.of("message", "继续", "sessionId", "own-session")));
        var argument = ArgumentCaptor.forClass(DeepSeekRequestDTO.class);
        verify(provider).chatCompletionSync(argument.capture());
        List<String> content = argument.getValue().getMessages().stream().map(DeepSeekRequestDTO.MessageDTO::getContent).toList();
        assertFalse(content.contains("turn-1"));
        assertFalse(content.contains("turn-2"));
        assertFalse(content.contains("private message"));
        assertTrue(content.containsAll(List.of("turn-3", "turn-4", "turn-5")));
    }

    @ParameterizedTest @ValueSource(strings = {"推荐商家", "推荐菜品", "川菜", "清淡"})
    void localHelpDoesNotInventCatalogItemsWhenTheDatabaseIsEmpty(String message) throws Exception {
        String reply = data(chat(Map.of("message", message))).path("message").asText();
        assertTrue(reply.contains("暂无") || reply.contains("未找到") || reply.contains("没有"), reply);
        assertFalse(reply.matches("(?s).*\\d+元.*"), reply);
        verifyNoInteractions(provider);
    }

    @Test void localHelpDoesNotPretendToConnectAHumanAgent() throws Exception {
        String reply = data(chat(Map.of("message", "投诉，转人工"))).path("message").asText();
        assertFalse(reply.contains("400-888-8888") || reply.contains("48小时") || reply.contains("立即为您记录"), reply);
        assertTrue(reply.contains("订单") && reply.contains("联系"), reply);
    }

    private ResultActions chat(Map<String, Object> body) throws Exception {
        return mvc.perform(post("/api/ai/chat").with(user("owner").authorities(() -> "USER"))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private JsonNode data(ResultActions response) throws Exception {
        return json.readTree(response.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString()).path("data");
    }

    private int count() { return jdbc.queryForObject("SELECT COUNT(*) FROM ai_chat_history", Integer.class); }

    private void seed(long id, long userId, String session, String message) {
        jdbc.update("INSERT INTO ai_chat_history(id,user_id,session_id,user_message,ai_response,chat_type,create_time,is_deleted) "
                + "VALUES (?,?,?,?,?,'general',?,0)",
                id, userId, session, message, "reply-" + id,
                java.time.LocalDateTime.of(2026, 1, 1, 0, 0).plusSeconds(id));
    }

    private DeepSeekResponseDTO modelReply(String content) {
        DeepSeekResponseDTO result = new DeepSeekResponseDTO();
        result.setChoices(List.of(new DeepSeekResponseDTO.ChoiceDTO(0,
                new DeepSeekResponseDTO.MessageDTO("assistant", content), "stop")));
        return result;
    }
}
