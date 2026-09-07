package com.tju.elm_bk.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-hardening;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
@ActiveProfiles("auth-test")
class AuthenticationHardeningTest {
    private static final String PASSWORD = "Study2026!";
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aWQAAAABJRU5ErkJggg==");
    @TempDir static Path uploads;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void isolatedUploads(DynamicPropertyRegistry properties) {
        properties.add("app.upload.directory", () -> uploads.toString());
    }

    @BeforeEach
    void reset() throws Exception {
        jdbc.update("DELETE FROM user_authority");
        jdbc.update("DELETE FROM person");
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM authority");
        for (String role : List.of("USER", "BUSINESS", "RIDER", "ADMIN")) {
            jdbc.update("INSERT INTO authority(name) VALUES (?)", role);
        }
        try (var files = Files.list(uploads)) {
            for (Path file : files.toList()) Files.delete(file);
        }
    }

    @Test
    void databaseItselfRejectsDuplicateUsername() throws Exception {
        register("unique", "13900000001");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO users(username,password,activated) VALUES ('unique','x',1)"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void databaseItselfRejectsDuplicatePhone() throws Exception {
        register("one", "13900000001");
        register("two", "13900000002");
        assertThatThrownBy(() -> jdbc.update("UPDATE person SET phone = '13900000001' WHERE phone = '13900000002'"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void concurrentRegistrationsOfOneUsernameHaveExactlyOneWinner() throws Exception {
        raceRegistrations(true);
    }

    @Test
    void concurrentRegistrationsOfOnePhoneHaveExactlyOneWinner() throws Exception {
        raceRegistrations(false);
    }

    @Test
    void uploadedAvatarIsStoredAndCanBeReadBack() throws Exception {
        var result = mvc.perform(multipart("/api/register").file(userPart()).file(
                        new MockMultipartFile("avatar", "../../avatar.png", "image/png", PNG)))
                .andExpect(status().isOk()).andReturn();
        String photo = json.readTree(result.getResponse().getContentAsByteArray()).at("/data/photo").asText();
        assertThat(photo).matches("/uploads/[0-9a-f-]+\\.png");
        mvc.perform(get(photo)).andExpect(status().isOk()).andExpect(content().bytes(PNG));
        assertThat(jdbc.queryForObject("SELECT photo FROM person", String.class)).isEqualTo(photo);
    }

    @Test
    void fakeImageCannotCreateAnAccountOrAFile() throws Exception {
        mvc.perform(multipart("/api/register").file(userPart()).file(new MockMultipartFile(
                        "avatar", "image.png", "image/png", "<svg onload='alert(1)'/>".getBytes())))
                .andExpect(status().isBadRequest());
        assertEmptyRegistration();
    }

    @Test
    void oversizedImageCannotCreateAnAccountOrAFile() throws Exception {
        mvc.perform(multipart("/api/register").file(userPart()).file(new MockMultipartFile(
                        "avatar", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1])))
                .andExpect(status().isBadRequest());
        assertEmptyRegistration();
    }

    @Test
    void missingMultipartUserIsAClientError() throws Exception {
        mvc.perform(multipart("/api/register").file(new MockMultipartFile("avatar", PNG)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        assertEmptyRegistration();
    }

    @Test
    void rollbackAlsoRemovesUploadedAvatar() throws Exception {
        jdbc.update("DELETE FROM authority WHERE name = 'USER'");
        mvc.perform(multipart("/api/register").file(userPart()).file(
                        new MockMultipartFile("avatar", "avatar.png", "image/png", PNG)))
                .andExpect(status().isInternalServerError());
        assertEmptyRegistration();
    }

    @Test
    void currentProfileUsesAuthenticatedIdentityNotQueryUserId() throws Exception {
        register("current", "13900000001");
        register("someone_else", "13900000002");
        String token = login("current", "user");
        Long other = jdbc.queryForObject("SELECT id FROM users WHERE username='someone_else'", Long.class);
        mvc.perform(get("/api/user").param("userId", other.toString()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("current"))
                .andExpect(jsonPath("$.phone").value("13900000001"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.authorities[0].name").value("USER"));
    }

    @Test
    void anonymousUserCannotReadCurrentProfile() throws Exception {
        mvc.perform(get("/api/user")).andExpect(status().isUnauthorized());
    }

    @Test
    void applicantTokenCannotBeReusedAfterApproval() throws Exception {
        register("candidate", "13900000001");
        String token = login("candidate", "rider");
        mvc.perform(get("/httpRest/success").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        jdbc.update("INSERT INTO user_authority SELECT id, 'RIDER' FROM users WHERE username='candidate'");
        mvc.perform(get("/httpRest/success").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void deletedAccountCannotKeepUsingItsToken() throws Exception {
        register("deleted", "13900000001");
        String token = login("deleted", "user");
        jdbc.update("UPDATE users SET is_deleted=1 WHERE username='deleted'");
        mvc.perform(get("/httpRest/success").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void accountUpdateInvalidatesOlderToken() throws Exception {
        register("updated", "13900000001");
        String token = login("updated", "user");
        jdbc.update("UPDATE users SET update_time=TIMESTAMP '2099-01-01 00:00:00' WHERE username='updated'");
        mvc.perform(get("/httpRest/success").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    private void raceRegistrations(boolean sameUsername) throws Exception {
        var executor = Executors.newFixedThreadPool(6);
        var ready = new CountDownLatch(6);
        var start = new CountDownLatch(1);
        List<Future<Integer>> requests = new ArrayList<>();
        try {
            for (int i = 0; i < 6; i++) {
                int index = i;
                requests.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Race did not start");
                    return mvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsBytes(body(sameUsername ? "racing" : "racing" + index,
                                    sameUsername ? "1390000000" + index : "13900000001"))))
                            .andReturn().getResponse().getStatus();
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (var request : requests) statuses.add(request.get(30, TimeUnit.SECONDS));
            assertThat(statuses).containsExactlyInAnyOrder(200, 409, 409, 409, 409, 409);
            for (String table : List.of("users", "person", "user_authority")) {
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).isEqualTo(1);
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void assertEmptyRegistration() throws Exception {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM person", Integer.class)).isZero();
        try (var files = Files.list(uploads)) { assertThat(files.count()).isZero(); }
    }

    private Map<String, String> body(String username, String phone) {
        return Map.of("username", username, "phone", phone, "password", PASSWORD);
    }

    private MockMultipartFile userPart() throws Exception {
        return new MockMultipartFile("user", "", "application/json", json.writeValueAsBytes(body("avatar_user", "13900000001")));
    }

    private void register(String username, String phone) throws Exception {
        mvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body(username, phone))))
                .andExpect(status().isOk());
    }

    private String login(String username, String role) throws Exception {
        var result = mvc.perform(post("/api/auth").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("username", username, "password", PASSWORD, "role", role))))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsByteArray()).get("id_token").asText();
    }
}
