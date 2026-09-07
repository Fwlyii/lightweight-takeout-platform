package com.tju.elm_bk.auth;

import com.tju.elm_bk.ElmBkApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Local-only UI acceptance server. Test classpath and ephemeral H2; never packaged in the app JAR. */
public class AuthPreviewApplication {
    public static void main(String[] args) {
        var application = new SpringApplication(ElmBkApplication.class);
        application.setAdditionalProfiles("auth-test");
        var context = application.run(
                "--spring.datasource.url=jdbc:h2:mem:auth-preview;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "--spring.datasource.driver-class-name=org.h2.Driver",
                "--spring.datasource.username=sa", "--spring.datasource.password=",
                "--app.upload.directory=" + System.getProperty("java.io.tmpdir") + "/lcw-auth-preview-uploads",
                "--server.port=8080");
        var jdbc = context.getBean(JdbcTemplate.class);
        String password = context.getBean(PasswordEncoder.class).encode("Study2026!");
        String[] roles = {"USER", "BUSINESS", "RIDER", "ADMIN"};
        String[] accounts = {"preview_user", "preview_merchant", "preview_rider", "preview_admin"};
        for (int i = 0; i < roles.length; i++) {
            jdbc.update("INSERT INTO authority(name) VALUES (?)", roles[i]);
            jdbc.update("INSERT INTO users(username,password,activated,is_deleted,create_time,update_time) "
                    + "VALUES(?,?,1,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", accounts[i], password);
            Long id = jdbc.queryForObject("SELECT id FROM users WHERE username=?", Long.class, accounts[i]);
            jdbc.update("INSERT INTO person(id,phone,photo) VALUES(?,?,?)", id, "1390000000" + i,
                    "/images/default-user-avatar.png");
            jdbc.update("INSERT INTO user_authority(user_id,authority_name) VALUES(?,?)", id, roles[i]);
        }
        System.out.println("Local authentication preview ready. Four preview_* accounts use Study2026!");
    }
}
