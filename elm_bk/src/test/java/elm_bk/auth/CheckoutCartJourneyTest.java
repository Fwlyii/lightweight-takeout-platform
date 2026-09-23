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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:checkout-cart;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
@ActiveProfiles("auth-test")
class CheckoutCartJourneyTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @BeforeEach void seed() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS business(id BIGINT PRIMARY KEY, business_name VARCHAR(100), is_deleted INT)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS food(id BIGINT PRIMARY KEY, food_name VARCHAR(100), food_price DECIMAL(10,2), food_img VARCHAR(100), stock INT, category VARCHAR(100), purchase_limit INT, is_deleted INT)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS cart(id BIGINT PRIMARY KEY, business_id BIGINT, food_id BIGINT, customer_id BIGINT, quantity INT, is_deleted INT, remarks VARCHAR(255))");
        jdbc.update("DELETE FROM cart"); jdbc.update("DELETE FROM food"); jdbc.update("DELETE FROM business");
        jdbc.update("DELETE FROM user_authority"); jdbc.update("DELETE FROM person"); jdbc.update("DELETE FROM users");
        jdbc.update("INSERT INTO users(id,username,password,activated,is_deleted) VALUES (11,'cart-owner','unused',1,0),(12,'other-owner','unused',1,0)");
        jdbc.update("INSERT INTO business VALUES (1,'商家一',0),(2,'商家二',0)");
        jdbc.update("INSERT INTO food VALUES (21,'商品一',10,'',10,'主食',9,0),(22,'商品二',12,'',10,'主食',9,0)");
        jdbc.update("INSERT INTO cart(id,business_id,food_id,customer_id,quantity,is_deleted) VALUES (1,1,21,11,1,0),(2,2,22,11,2,0),(3,1,21,12,3,0),(4,1,21,11,4,1)");
    }
    @Test @WithMockUser(username="cart-owner", authorities="USER")
    void globalCartReadsOnlyCurrentUsersActiveRowsAcrossMerchants() throws Exception {
        mvc.perform(get("/api/carts/list")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[1].id").value(2));
        mvc.perform(get("/api/carts/list?businessId=2")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(2));
    }
    @Test @WithMockUser(username="other-owner", authorities="USER")
    void requestCannotOverrideCartOwner() throws Exception {
        mvc.perform(get("/api/carts/list?userId=11")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(3));
    }
    @Test void guestCannotReadGlobalCart() throws Exception {
        mvc.perform(get("/api/carts/list")).andExpect(status().isUnauthorized());
    }
}
