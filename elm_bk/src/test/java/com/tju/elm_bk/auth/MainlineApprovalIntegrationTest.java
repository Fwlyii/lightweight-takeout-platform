package com.tju.elm_bk.auth;

import com.tju.elm_bk.dto.RiderAuditDTO;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.service.RiderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real Spring transaction/security proxies and MyBatis SQL; no demo database or service mocks. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mainline_approval;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.schema-locations=classpath:auth-schema.sql,classpath:mainline-approval-schema.sql"
})
@AutoConfigureMockMvc
@ActiveProfiles("auth-test")
class MainlineApprovalIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired RiderService riders;
    @Autowired MockMvc mvc;

    @BeforeEach
    void seedIsolatedDatabase() {
        jdbc.update("DELETE FROM notification");
        jdbc.update("DELETE FROM rider_profile");
        jdbc.update("DELETE FROM user_authority");
        jdbc.update("DELETE FROM person");
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM authority");
        jdbc.update("INSERT INTO authority(name) VALUES ('USER'), ('RIDER'), ('ADMIN')");
        jdbc.update("INSERT INTO users(id, username, password, activated, is_deleted) VALUES (12, 'applicant', 'test-only', 1, 0)");
        jdbc.update("INSERT INTO user_authority(user_id, authority_name) VALUES (12, 'USER')");
        jdbc.update("INSERT INTO rider_profile(id, user_id, real_name, phone, vehicle_type) VALUES (1, 12, '测试骑手', '13900000000', '电动车')");
    }

    private RiderAuditDTO approval() {
        RiderAuditDTO dto = new RiderAuditDTO();
        dto.setApproved(true);
        return dto;
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void successfulApprovalCommitsRoleStatusAndMessageTogether() {
        assertEquals(1, riders.audit(1L, approval()).getAuditStatus());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM user_authority WHERE user_id=12 AND authority_name='RIDER'", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE user_id=12 AND audit_result=1", Integer.class));
        assertThrows(APIException.class, () -> riders.audit(1L, approval()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM notification", Integer.class));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void missingRoleConfigurationRollsBackPreviouslyUpdatedAuditRow() {
        jdbc.update("DELETE FROM authority WHERE name='RIDER'");
        assertThrows(APIException.class, () -> riders.audit(1L, approval()));
        assertEquals(0, jdbc.queryForObject("SELECT audit_status FROM rider_profile WHERE id=1", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM notification", Integer.class));
    }

    @Test
    @WithMockUser(authorities = "USER")
    void nonAdminCannotBypassControllerAndInvokeAuditService() {
        assertThrows(AccessDeniedException.class, () -> riders.audit(1L, approval()));
        assertThrows(AccessDeniedException.class, () -> riders.listApplications(null));
        assertEquals(0, jdbc.queryForObject("SELECT audit_status FROM rider_profile WHERE id=1", Integer.class));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void adminGetsExplicitUnavailableResponseForFutureDeliveryModule() throws Exception {
        mvc.perform(get("/api/v1/admin/delivery-exceptions"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("FEATURE_UNAVAILABLE"));
    }

    @Test
    @WithMockUser(authorities = "USER")
    void unavailableEndpointStillEnforcesAdminPermission() throws Exception {
        mvc.perform(get("/api/v1/admin/delivery-exceptions")).andExpect(status().isForbidden());
    }
}
