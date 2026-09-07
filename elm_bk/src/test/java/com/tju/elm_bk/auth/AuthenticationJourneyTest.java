package com.tju.elm_bk.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP acceptance tests: real Spring filters, controllers, services, MyBatis and H2. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:authentication;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
@ActiveProfiles("auth-test")
class AuthenticationJourneyTest {
    private static final String PASSWORD = "Study2026!";
    private static final String DEFAULT_AVATAR = "/images/default-user-avatar.png";
    private static final BCryptPasswordEncoder HASHER = new BCryptPasswordEncoder(4);

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Value("${jwt.secret}") String signingKey;

    @BeforeEach
    void resetOnlyTheInMemoryAuthenticationTables() {
        jdbc.update("DELETE FROM user_authority");
        jdbc.update("DELETE FROM person");
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM authority");
        for (String role : List.of("USER", "BUSINESS", "RIDER", "ADMIN")) {
            jdbc.update("INSERT INTO authority(name) VALUES (?)", role);
        }
    }

    @Test
    void registrationWithoutAvatarStoresDefaultAvatarAndHashedPassword() throws Exception {
        register(registration()).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.photo").value(DEFAULT_AVATAR))
                .andExpect(jsonPath("$.data.password").doesNotExist());

        String hash = jdbc.queryForObject("SELECT password FROM users WHERE username = 'new_user'", String.class);
        assertThat(hash).isNotEqualTo(PASSWORD);
        assertThat(HASHER.matches(PASSWORD, hash)).isTrue();
        assertThat(jdbc.queryForObject("SELECT photo FROM person", String.class)).isEqualTo(DEFAULT_AVATAR);
        assertThat(jdbc.queryForList("SELECT authority_name FROM user_authority", String.class))
                .containsExactly("USER");
    }

    @Test
    void multipartRegistrationAlsoAllowsOmittingTheAvatar() throws Exception {
        MockMultipartFile user = new MockMultipartFile("user", "", "application/json",
                json.writeValueAsBytes(registration()));
        mvc.perform(multipart("/api/register").file(user)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.photo").value(DEFAULT_AVATAR));
    }

    @Test
    void clientCannotAssignAdministrativeAuthorityOrAnArbitraryAvatarUrl() throws Exception {
        Map<String, Object> body = registration();
        body.put("authorities", List.of(Map.of("name", "ADMIN")));
        body.put("photo", "https://invalid.example/untrusted-avatar.png");
        body.put("deleted", true);
        body.put("activated", false);
        register(body).andExpect(status().isOk()).andExpect(jsonPath("$.data.photo").value(DEFAULT_AVATAR));
        assertThat(jdbc.queryForList("SELECT authority_name FROM user_authority", String.class))
                .containsExactly("USER");
        assertThat(jdbc.queryForObject("SELECT activated FROM users", Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM users", Boolean.class)).isFalse();
    }

    @Test
    void registrationCanBeFollowedByARealLoginAndAuthenticatedRequest() throws Exception {
        register(registration()).andExpect(status().isOk());
        String token = successfulLogin("new_user", "user");
        mvc.perform(get("/httpRest/success").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void registeredPhoneCanAlsoBeUsedToLogInAsRequiredByTheSrs() throws Exception {
        register(registration()).andExpect(status().isOk());
        String token = successfulLogin("13900000001", "user");
        assertThat(verifySignature(token).getSubject()).isEqualTo("new_user");
    }

    @Test
    void duplicateUsernameIsRejectedWithoutCreatingAnotherAccount() throws Exception {
        register(registration()).andExpect(status().isOk());
        Map<String, Object> duplicate = registration();
        duplicate.put("phone", "13900000002");
        register(duplicate).andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(1);
    }

    @Test
    void duplicatePhoneIsRejectedWithoutCreatingAnOrphanAccount() throws Exception {
        register(registration()).andExpect(status().isOk());
        Map<String, Object> duplicate = registration();
        duplicate.put("username", "second_user");
        register(duplicate).andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM person", Integer.class)).isEqualTo(1);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123", "12900000000", "139000000000", "1390000000a"})
    void invalidPhoneDoesNotWriteAnAccount(String phone) throws Exception {
        Map<String, Object> body = registration();
        body.put("phone", phone);
        register(body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Short1", "abcdefgh", "12345678", "Abcdefghijklmnopqrstuvwxyz12345678"})
    void invalidPasswordDoesNotWriteAnAccount(String password) throws Exception {
        Map<String, Object> body = registration();
        body.put("password", password);
        register(body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "username_over_twenty_characters"})
    void invalidUsernameIsRejected(String username) throws Exception {
        Map<String, Object> body = registration();
        body.put("username", username);
        register(body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void registrationTrimsUsernameBeforeCheckingUniqueness() throws Exception {
        register(registration()).andExpect(status().isOk());
        Map<String, Object> duplicate = registration();
        duplicate.put("username", " new_user ");
        duplicate.put("phone", "13900000002");
        register(duplicate).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(1);
    }

    @Test
    void failureToAssignBaseAuthorityRollsBackTheWholeRegistration() throws Exception {
        jdbc.update("DELETE FROM authority WHERE name = 'USER'");
        register(registration()).andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.success").value(false));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM person", Integer.class)).isZero();
    }

    @ParameterizedTest
    @CsvSource({"USER,user", "BUSINESS,merchant", "RIDER,rider", "ADMIN,admin"})
    void eachApprovedRoleLogsInThroughItsOwnPortalWithOneSignedAuthority(String authority, String portal)
            throws Exception {
        seedAccount("approved", true, authority);
        Claims claims = verifySignature(successfulLogin("approved", portal));
        assertThat(claims.getSubject()).isEqualTo("approved");
        assertThat(claims.get("auth", String.class)).isEqualTo(authority);
        assertThat(claims.get("session_role", String.class)).isEqualTo(portal);
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @ParameterizedTest
    @CsvSource({"RIDER,user", "BUSINESS,user", "ADMIN,user", "BUSINESS,rider", "RIDER,merchant", "USER,admin"})
    void serverRejectsAValidPasswordUsedAtTheWrongPortal(String authority, String portal) throws Exception {
        seedAccount("wrong_portal", true, authority);
        login("wrong_portal", PASSWORD, portal).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.id_token").doesNotExist());
    }

    @Test
    void riderWithLegacyBaseUserAuthorityStillCannotEnterCustomerPortal() throws Exception {
        seedAccount("legacy_rider", true, "USER", "RIDER");
        login("legacy_rider", PASSWORD, "user").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
        assertThat(verifySignature(successfulLogin("legacy_rider", "rider")).get("auth", String.class))
                .isEqualTo("RIDER");
    }

    @ParameterizedTest
    @CsvSource({"merchant,BUSINESS_APPLICANT", "rider,RIDER_APPLICANT"})
    void customerCanOnlyGetApplicantPermissionAtAnOperationalPortal(String portal, String authority)
            throws Exception {
        seedAccount("applicant", true, "USER");
        JsonNode response = response(login("applicant", PASSWORD, portal).andExpect(status().isOk())
                .andExpect(jsonPath("$.application_only").value(true)));
        assertThat(verifySignature(response.get("id_token").asText()).get("auth", String.class))
                .isEqualTo(authority);
    }

    @Test
    void wrongPasswordAndUnknownAccountReturnTheSameAuthenticationFailure() throws Exception {
        seedAccount("existing", true, "USER");
        JsonNode wrong = response(login("existing", "Wrong2026!", "user").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false)));
        JsonNode absent = response(login("absent", PASSWORD, "user").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false)));
        assertThat(wrong.get("message")).isEqualTo(absent.get("message"));
    }

    @Test
    void disabledAccountCannotReceiveAToken() throws Exception {
        seedAccount("disabled", false, "USER");
        login("disabled", PASSWORD, "user").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.id_token").doesNotExist());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"owner", "ADMINISTRATOR"})
    void missingOrUnknownPortalIsRejected(String portal) throws Exception {
        seedAccount("existing", true, "USER");
        login("existing", PASSWORD, portal).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void changingTheAccountStatusInvalidatesAnAlreadyIssuedToken() throws Exception {
        seedAccount("revoked", true, "USER");
        String token = successfulLogin("revoked", "user");
        jdbc.update("UPDATE users SET activated = 0 WHERE username = 'revoked'");
        mvc.perform(get("/httpRest/success").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokensAreRejectedEvenWhenTheirSignatureIsCorrect() throws Exception {
        seedAccount("expired", true, "USER");
        String token = Jwts.builder().setSubject("expired").claim("auth", "USER")
                .claim("session_role", "user").setExpiration(new Date(1))
                .signWith(Keys.hmacShaKeyFor(signingKey.getBytes(StandardCharsets.UTF_8))).compact();
        mvc.perform(get("/httpRest/success").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private Map<String, Object> registration() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", "new_user");
        body.put("password", PASSWORD);
        body.put("phone", "13900000001");
        return body;
    }

    private ResultActions register(Map<String, Object> body) throws Exception {
        return mvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body)));
    }

    private ResultActions login(String username, String password, String portal) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("role", portal);
        return mvc.perform(post("/api/auth").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body)));
    }

    private String successfulLogin(String username, String portal) throws Exception {
        return response(login(username, PASSWORD, portal).andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(portal))
                .andExpect(jsonPath("$.id_token").isNotEmpty())).get("id_token").asText();
    }

    private JsonNode response(ResultActions action) throws Exception {
        return json.readTree(action.andReturn().getResponse().getContentAsByteArray());
    }

    private Claims verifySignature(String token) {
        return Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(signingKey.getBytes(StandardCharsets.UTF_8)))
                .build().parseClaimsJws(token).getBody();
    }

    private void seedAccount(String username, boolean activated, String... authorities) {
        jdbc.update("INSERT INTO users(username, password, activated, is_deleted, create_time, update_time) "
                        + "VALUES (?, ?, ?, 0, TIMESTAMP '2026-01-01 00:00:00', TIMESTAMP '2026-01-01 00:00:00')",
                username, HASHER.encode(PASSWORD), activated ? 1 : 0);
        Long id = jdbc.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
        for (String authority : authorities) {
            jdbc.update("INSERT INTO user_authority(user_id, authority_name) VALUES (?, ?)", id, authority);
        }
    }
}
