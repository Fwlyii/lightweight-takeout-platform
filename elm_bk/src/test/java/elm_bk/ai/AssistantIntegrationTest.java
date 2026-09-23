package elm_bk.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import elm_bk.adapter.ImageRecognitionAdapter;
import elm_bk.adapter.SpeechAdapter;
import elm_bk.mapper.AiChatHistoryMapper;
import elm_bk.utils.DeepSeekApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=${assistant.test.jdbc-url:jdbc:h2:mem:assistant_integration;MODE=MySQL;DB_CLOSE_DELAY=-1}",
    "spring.datasource.driver-class-name=${assistant.test.jdbc-driver:org.h2.Driver}",
    "spring.datasource.username=${assistant.test.jdbc-user:sa}",
    "spring.datasource.password=${assistant.test.jdbc-password:}",
    "spring.sql.init.mode=${assistant.test.init-mode:always}",
    "spring.sql.init.schema-locations=classpath:auth-schema.sql,classpath:order-snapshot-schema.sql,classpath:ai-history-schema.sql,classpath:assistant-extra-schema.sql"
})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("auth-test")
class AssistantIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @SpyBean AiChatHistoryMapper history;
    @MockBean DeepSeekApiClient model;
    @MockBean ImageRecognitionAdapter images;
    @MockBean SpeechAdapter speech;

    @BeforeEach void prepare() throws Exception {
        try (var connection = jdbc.getDataSource().getConnection()) {
            assertTrue("assistant_integration".equalsIgnoreCase(connection.getCatalog())
                    || "assistant_integration_test".equals(connection.getCatalog()), "仅允许独立测试数据库");
        }
        for (String table : new String[]{"review", "user_preference", "ai_chat_history", "notification",
                "user_asset_ledger", "user_coupon", "order_status_history", "orderdetailet", "orders", "cart",
                "user_asset", "food", "business", "delivery_address", "user_authority", "person", "users", "authority"}) {
            jdbc.update("DELETE FROM " + table);
        }
        jdbc.update("INSERT INTO authority(name) VALUES ('USER')");
        jdbc.update("INSERT INTO users(id,username,password,activated,is_deleted) VALUES (1,'owner','test-only',1,0),(2,'other','test-only',1,0)");
        jdbc.update("INSERT INTO user_authority(user_id,authority_name) VALUES (1,'USER'),(2,'USER')");
        jdbc.update("INSERT INTO business(id,user_id,business_name,start_price,delivery_price,status,operating_status,is_deleted) VALUES (10,1,'真实测试店',0,2,1,1,0),(11,2,'休息店',0,2,1,0,0)");
        jdbc.update("INSERT INTO food(id,business_id,food_name,food_price,stock,shelve_status,is_deleted) VALUES (101,10,'清淡牛肉面',20,5,1,0),(102,10,'售罄牛肉面',10,0,1,0),(103,11,'休息店牛肉面',10,5,1,0)");
        when(images.provider()).thenReturn("not-configured");
        when(speech.provider()).thenReturn("not-configured");
    }

    @Test void structuredRecommendationsUseRealAvailableInventoryAndPersistOnce() throws Exception {
        mvc.perform(post("/api/v1/assistant/messages").with(user("owner").authorities(() -> "USER"))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("message", "推荐牛肉面", "budget", 25))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.intent").value("RECOMMENDATION"))
                .andExpect(jsonPath("$.data.candidates.length()").value(1))
                .andExpect(jsonPath("$.data.candidates[0].foodId").value(101))
                .andExpect(jsonPath("$.data.needConfirmation").value(true));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM ai_chat_history WHERE user_id=1", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM cart", Integer.class));
        verifyNoInteractions(model);
    }

    @Test void recommendationQuantityParticipatesInBudgetFiltering() throws Exception {
        mvc.perform(post("/api/v1/recommendations").with(user("owner").authorities(() -> "USER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"query\":\"牛肉面\",\"quantity\":2,\"budget\":25}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test void anotherUsersStructuredSessionIsRejectedBeforeGeneration() throws Exception {
        jdbc.update("INSERT INTO ai_chat_history(user_id,session_id,user_message,ai_response,is_deleted) VALUES (2,'private','secret','secret',0)");
        mvc.perform(post("/api/v1/assistant/messages").with(user("owner").authorities(() -> "USER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"推荐牛肉面\",\"sessionId\":\"private\"}"))
                .andExpect(status().isForbidden());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM ai_chat_history", Integer.class));
    }

    @Test void storageFailureCannotBeReportedAsASuccessfulConversation() throws Exception {
        doThrow(new IllegalStateException("simulated database failure")).when(history).insert(any());
        mvc.perform(post("/api/v1/assistant/messages").with(user("owner").authorities(() -> "USER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"退款规则\"}"))
                .andExpect(status().is5xxServerError());
    }

    @Test void allLocalStructuredRepliesUseTheSharedHistorySource() throws Exception {
        mvc.perform(post("/api/v1/assistant/messages").with(user("owner").authorities(() -> "USER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"退款规则\"}"))
                .andExpect(status().isOk());
        var context = json.readTree(jdbc.queryForObject("SELECT context_data FROM ai_chat_history", String.class));
        assertEquals("local", context.path("source").asText());
    }

    @Test void anonymousStructuredRequestsCannotReadPreferencesOrWriteHistory() throws Exception {
        mvc.perform(post("/api/v1/assistant/messages").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"推荐牛肉面\"}")).andExpect(status().isUnauthorized());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM ai_chat_history", Integer.class));
    }

    @Test void blankMessagesAndInvalidQuantitiesAreRejected() throws Exception {
        mvc.perform(post("/api/v1/assistant/messages").with(user("owner").authorities(() -> "USER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"  \"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/recommendations").with(user("owner").authorities(() -> "USER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":0}")).andExpect(status().isBadRequest());
    }

    @Test void unconfiguredMediaCapabilitiesAreNotPresentedAsAvailable() throws Exception {
        mvc.perform(get("/api/v1/assistant/capabilities").with(user("owner").authorities(() -> "USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.imageRecognition").value(false))
                .andExpect(jsonPath("$.data.speechRecognition").value(false));
        verifyNoInteractions(model);
    }
}
