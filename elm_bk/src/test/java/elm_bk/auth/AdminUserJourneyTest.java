package elm_bk.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:admin-users;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password="})
@AutoConfigureMockMvc
@ActiveProfiles("auth-test")
class AdminUserJourneyTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @BeforeEach void seed() {
        jdbc.update("DELETE FROM user_authority"); jdbc.update("DELETE FROM person"); jdbc.update("DELETE FROM users");
        jdbc.update("MERGE INTO authority KEY(name) VALUES ('ADMIN'),('USER')");
        jdbc.update("INSERT INTO users(id,username,password,activated,is_deleted) VALUES (11,'admin-test','secret',1,0),(12,'customer-test','secret',1,0),(13,'disabled-test','secret',0,0)");
        jdbc.update("INSERT INTO user_authority VALUES (11,'ADMIN'),(12,'USER'),(13,'USER')");
        jdbc.update("INSERT INTO person(id,phone) VALUES (12,'13900008888')");
    }
    @Test @WithMockUser(username="admin-test", authorities="ADMIN")
    void listSearchAndStatusFilterExcludeCredentials() throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].password").doesNotExist());
        mvc.perform(get("/api/admin/users?status=2")).andExpect(jsonPath("$.data[0].id").value(13));
        mvc.perform(get("/api/admin/users?keyword=13900008888")).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(12));
    }
    @Test @WithMockUser(username="admin-test", authorities="ADMIN")
    void canToggleCustomerButNotAdmin() throws Exception {
        mvc.perform(put("/api/admin/users/12/status?activated=false")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        assertEquals(0,jdbc.queryForObject("SELECT activated FROM users WHERE id=12",Integer.class));
        mvc.perform(put("/api/admin/users/12/status?activated=true")).andExpect(status().isOk());
        assertEquals(1,jdbc.queryForObject("SELECT activated FROM users WHERE id=12",Integer.class));
        mvc.perform(put("/api/admin/users/11/status?activated=false")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("不能禁用自己或管理员账户"));
        assertEquals(1,jdbc.queryForObject("SELECT activated FROM users WHERE id=11",Integer.class));
    }
    @Test @WithMockUser(username="customer-test", authorities="USER")
    void customerCannotListOrChangeAccounts() throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/users/13/status?activated=true")).andExpect(status().isForbidden());
    }
    @Test void guestCannotListAccounts() throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }
}
