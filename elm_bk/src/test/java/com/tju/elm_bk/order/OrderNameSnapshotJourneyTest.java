package com.tju.elm_bk.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 不模拟业务层和数据库：准备数据 → 调用真实接口 → 检查订单与数据库。 */
@SpringBootTest(properties = {
    "spring.datasource.url=${snapshot.test.jdbc-url:jdbc:h2:mem:order_snapshot;MODE=MySQL;DB_CLOSE_DELAY=-1}",
    "spring.datasource.driver-class-name=${snapshot.test.jdbc-driver:org.h2.Driver}",
    "spring.datasource.username=${snapshot.test.jdbc-user:sa}",
    "spring.datasource.password=${snapshot.test.jdbc-password:}",
    "spring.sql.init.mode=${snapshot.test.init-mode:always}",
    "spring.sql.init.schema-locations=classpath:auth-schema.sql,classpath:order-snapshot-schema.sql",
    "app.demo.enabled=true"
})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("auth-test")
class OrderNameSnapshotJourneyTest {
    private static final long FOOD = 1001;
    private static final String ORIGINAL_NAME = "番茄炒蛋";
    private static final String NEW_NAME = "红烧牛肉";
    private static final String LEGACY_NAME = "历史商品（名称未留存）";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;

    @BeforeEach
    void prepareUsersShopAndMenu() throws Exception {
        // 防止误把测试清理动作运行到演示库：只接受这两个专用数据库名。
        try (var connection = jdbc.getDataSource().getConnection()) {
            String catalog = connection.getCatalog();
            assertTrue("ORDER_SNAPSHOT".equalsIgnoreCase(catalog)
                    || "order_snapshot_test".equals(catalog), "只允许使用订单快照专用测试库");
        }
        for (String table : new String[]{"notification", "user_asset_ledger", "user_coupon",
                "order_status_history", "orderdetailet", "orders", "cart", "user_asset",
                "food", "business", "delivery_address", "user_authority", "person", "users", "authority"}) {
            jdbc.update("DELETE FROM " + table);
        }
        jdbc.update("INSERT INTO authority(name) VALUES ('USER'),('BUSINESS')");
        jdbc.update("INSERT INTO users(id,username,password,activated,is_deleted) VALUES "
                + "(1,'customer','test-only',1,0),(2,'merchant','test-only',1,0),"
                + "(3,'otherCustomer','test-only',1,0),(4,'otherMerchant','test-only',1,0)");
        jdbc.update("INSERT INTO user_authority(user_id,authority_name) VALUES "
                + "(1,'USER'),(2,'BUSINESS'),(3,'USER'),(4,'BUSINESS')");
        jdbc.update("INSERT INTO business(id,user_id,business_name,status,operating_status,is_deleted,"
                + "start_price,delivery_price,dine_in_available) VALUES (101,2,'测试餐厅',1,1,0,0,3,1)");
        jdbc.update("INSERT INTO food(id,business_id,food_name,food_price,stock,category,shelve_status,is_deleted) "
                + "VALUES (1001,101,?,20,100,'热菜',1,0),(1002,101,'米饭',3,100,'主食',1,0)", ORIGINAL_NAME);
        jdbc.update("INSERT INTO delivery_address(id,user_id,address,contact_name,contact_tel,is_deleted) "
                + "VALUES (301,1,'测试校区食堂门口','测试顾客','13900000001',0)");
    }

    @ParameterizedTest
    @ValueSource(strings = {"pickup", "delivery"})
    @DisplayName("正常下单保存服务器上的名称、单价与数量")
    void newOrderHasCorrectNamePriceAndQuantity(String mode) throws Exception {
        addToCart(FOOD, 2);
        long orderId = submit(mode, "normal");

        assertItem(orderFoods(orderId, "customer"), FOOD, ORIGINAL_NAME, "20.00", 2);
        var order = data(as("customer", get("/api/orders/detail").param("orderId", "" + orderId)));
        assertEquals(new BigDecimal("pickup".equals(mode) ? "40.00" : "43.00"),
                order.path("orderTotal").decimalValue().setScale(2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"customer", "merchant", "merchant-list", "legacy"})
    @DisplayName("商家改名改价后，各订单查询入口仍返回原名称与原价格")
    void renamedMenuCannotRewritePendingOrder(String view) throws Exception {
        addToCart(FOOD, 1);
        long orderId = submit("pickup", "rename");

        renameFood(FOOD, NEW_NAME, "35.00");

        assertItem(orderFoods(orderId, view), FOOD, ORIGINAL_NAME, "20.00", 1);
    }

    @Test
    @DisplayName("已支付并完成的订单也保留下单时的菜名")
    void completedOrderKeepsItsOriginalName() throws Exception {
        addToCart(FOOD, 1);
        long orderId = submit("pickup", "completed");
        as("customer", put("/api/orders/status").param("orderId", "" + orderId)
                .param("orderState", "1").param("paymentMethod", "simulated")).andExpect(status().isOk());
        as("merchant", post("/api/v1/orders/" + orderId + "/merchant-accept")).andExpect(status().isOk());
        as("merchant", post("/api/v1/orders/" + orderId + "/merchant-ready")).andExpect(status().isOk());
        as("customer", post("/api/v1/orders/" + orderId + "/confirm-receipt")).andExpect(status().isOk());
        assertEquals(7, jdbc.queryForObject("SELECT order_state FROM orders WHERE id=?", Integer.class, orderId));

        renameFood(FOOD, NEW_NAME, "35.00");

        assertItem(orderFoods(orderId, "customer"), FOOD, ORIGINAL_NAME, "20.00", 1);
    }

    @Test
    @DisplayName("旧订单用旧名称，新订单用新名称，不能把菜单一起冻结")
    void oldAndNewOrdersKeepTheirOwnNamesAndPrices() throws Exception {
        addToCart(FOOD, 1);
        long oldOrder = submit("pickup", "old");
        renameFood(FOOD, NEW_NAME, "35.00");
        addToCart(FOOD, 2);
        long newOrder = submit("pickup", "new");

        assertAll(
                () -> assertItem(orderFoods(oldOrder, "customer"), FOOD, ORIGINAL_NAME, "20.00", 1),
                () -> assertItem(orderFoods(newOrder, "customer"), FOOD, NEW_NAME, "35.00", 2));
    }

    @ParameterizedTest
    @CsvSource({"unlist,customer", "unlist,merchant-list", "unlist,legacy",
            "delete,customer", "delete,merchant-list", "delete,legacy"})
    @DisplayName("商品下架或逻辑删除后，订单明细仍可查到")
    void unavailableFoodRemainsInOrderHistory(String action, String view) throws Exception {
        addToCart(FOOD, 1);
        long orderId = submit("pickup", "unavailable");
        renameFood(FOOD, NEW_NAME, "35.00");
        if ("delete".equals(action)) {
            as("merchant", get("/api/foods/delete").param("foodId", "" + FOOD)).andExpect(status().isOk());
        } else {
            as("merchant", get("/api/foods/status").param("foodId", "" + FOOD)
                    .param("shelveStatus", "0")).andExpect(status().isOk());
        }

        assertItem(orderFoods(orderId, view), FOOD, ORIGINAL_NAME, "20.00", 1);
    }

    @Test
    @DisplayName("多商品订单、反复改名，不会串行或覆盖快照")
    void multipleItemsAndRepeatedRenamesKeepEachSnapshot() throws Exception {
        addToCart(FOOD, 2);
        addToCart(1002, 3);
        long orderId = submit("pickup", "multiple");
        renameFood(FOOD, NEW_NAME, "35.00");
        renameFood(FOOD, "清蒸鱼", "40.00");
        renameFood(1002, "炒饭", "8.00");

        var foods = orderFoods(orderId, "customer");
        assertEquals(2, foods.size());
        assertAll(
                () -> assertItem(foods, FOOD, ORIGINAL_NAME, "20.00", 2),
                () -> assertItem(foods, 1002, "米饭", "3.00", 3));
    }

    @ParameterizedTest
    @ValueSource(strings = {"番茄炒蛋（微辣）", "Chef's special", "MAX_LENGTH"})
    @DisplayName("中文、引号和允许的最长菜名都原样保存")
    void supportedNamesArePreservedExactly(String input) throws Exception {
        String name = "MAX_LENGTH".equals(input) ? "菜".repeat(100) : input;
        renameFood(FOOD, name, "20.00");
        addToCart(FOOD, 1);
        long orderId = submit("pickup", "name-boundary");
        renameFood(FOOD, NEW_NAME, "35.00");

        assertItem(orderFoods(orderId, "customer"), FOOD, name, "20.00", 1);
    }

    @Test
    @DisplayName("客户端伪造名称与价格不会影响订单快照")
    void clientCannotSupplyTheSnapshotNameOrPrice() throws Exception {
        addToCart(FOOD, 1);
        long orderId = data(as("customer", post("/api/orders/submit")
                .param("businessId", "101").param("serviceMode", "pickup")
                .param("foodName", "伪造菜名").param("foodPrice", "0.01")
                .param("orderTotal", "0.01"))).asLong();

        assertItem(orderFoods(orderId, "customer"), FOOD, ORIGINAL_NAME, "20.00", 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unchanged", "renamed", "deleted"})
    @DisplayName("没有原菜名的老订单明确提示缺失，不拿当前菜名冒充")
    void legacyOrdersHaveAnExplicitMissingNameLabel(String menuState) throws Exception {
        // 模拟升级前的老记录：插入时没有提供名称快照字段。
        jdbc.update("INSERT INTO orders(id,business_id,customer_id,order_total,delivery_price,order_state,"
                + "service_mode,is_deleted) VALUES (9001,101,1,20,0,7,'PICKUP',0)");
        jdbc.update("INSERT INTO orderdetailet(order_id,food_id,food_price,quantity,is_deleted) VALUES (9001,1001,20,1,0)");
        if (!"unchanged".equals(menuState)) renameFood(FOOD, NEW_NAME, "35.00");
        if ("deleted".equals(menuState)) {
            as("merchant", get("/api/foods/delete").param("foodId", "" + FOOD)).andExpect(status().isOk());
        }

        assertAll(
                () -> assertItem(orderFoods(9001, "customer"), FOOD, LEGACY_NAME, "20.00", 1),
                () -> assertItem(orderFoods(9001, "legacy"), FOOD, LEGACY_NAME, "20.00", 1));
    }

    @ParameterizedTest
    @CsvSource({"otherCustomer,detail", "otherCustomer,merchant-list", "otherCustomer,legacy",
            "otherMerchant,detail", "otherMerchant,merchant-list", "otherMerchant,legacy"})
    @DisplayName("其他顾客或商家不能读取本订单商品信息")
    void unrelatedAccountsCannotReadOrderFoods(String actor, String view) throws Exception {
        addToCart(FOOD, 1);
        long orderId = submit("pickup", "permissions");
        as(actor, readRequest(orderId, view)).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"detail", "merchant-list", "legacy"})
    @DisplayName("未登录不能读取订单信息")
    void anonymousCannotReadOrderFoods(String view) throws Exception {
        mvc.perform(readRequest(1, view)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"detail", "legacy"})
    @DisplayName("不存在的订单返回404")
    void missingOrderReturnsNotFound(String view) throws Exception {
        as("customer", readRequest(999999, view)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("第二种商品库存不足时，整笔下单回滚，不留半笔记录")
    void failedSubmissionLeavesNoOrderOrSnapshot() throws Exception {
        addToCart(FOOD, 1);
        addToCart(1002, 1);
        as("merchant", put("/api/foods/1002/stock").contentType(MediaType.APPLICATION_JSON)
                .content("{\"stock\":0}")).andExpect(status().isOk());

        as("customer", post("/api/orders/submit").param("businessId", "101")
                .param("serviceMode", "pickup")).andExpect(status().isConflict());

        assertAll(
                () -> assertEquals(0, count("orders")),
                () -> assertEquals(0, count("orderdetailet")),
                () -> assertEquals(0, count("order_status_history")),
                () -> assertEquals(100, jdbc.queryForObject("SELECT stock FROM food WHERE id=1001", Integer.class)),
                () -> assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM cart WHERE is_deleted=0", Integer.class)));
    }

    @Test
    @DisplayName("重试同一订单请求不会重新抓取新菜名")
    void repeatedSubmissionKeepsTheOriginalSnapshot() throws Exception {
        addToCart(FOOD, 1);
        long orderId = submit("pickup", "same-request");
        renameFood(FOOD, NEW_NAME, "35.00");

        assertEquals(orderId, submit("pickup", "same-request"));
        assertEquals(1, count("orders"));
        assertEquals(1, count("orderdetailet"));
        assertItem(orderFoods(orderId, "customer"), FOOD, ORIGINAL_NAME, "20.00", 1);
    }

    @Test
    @DisplayName("名称确实存进订单明细，不是临时缓存或页面假象")
    void snapshotNameIsPersistedOnTheOrderLine() throws Exception {
        addToCart(FOOD, 1);
        long orderId = submit("pickup", "persisted");

        var row = jdbc.queryForMap("SELECT * FROM orderdetailet WHERE order_id=?", orderId);
        assertEquals(ORIGINAL_NAME, row.get("food_name_snapshot"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/foods?business=101", "/api/foods/list?businessId=101"})
    @DisplayName("菜单查询仍显示当前名称与价格")
    void menuStillShowsCurrentProductInformation(String path) throws Exception {
        renameFood(FOOD, NEW_NAME, "35.00");
        assertItem(data(as("customer", get(path))), FOOD, NEW_NAME, "35.00", 1);
    }

    // 以下只是发请求、取JSON的辅助方法；上面的测试保留清晰的业务步骤。
    private ResultActions as(String actor, MockHttpServletRequestBuilder request) throws Exception {
        String authority = actor.toLowerCase().contains("merchant") ? "BUSINESS" : "USER";
        return mvc.perform(request.with(user(actor).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(authority))));
    }

    private JsonNode data(ResultActions response) throws Exception {
        String body = response.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data");
    }

    private void addToCart(long foodId, int quantity) throws Exception {
        data(as("customer", post("/api/carts/items").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("foodId", foodId, "quantity", quantity)))));
    }

    private long submit(String mode, String key) throws Exception {
        return data(as("customer", post("/api/orders/submit").param("businessId", "101")
                .param("serviceMode", mode).param("addressId", "301").header("Idempotency-Key", key))).asLong();
    }

    private void renameFood(long foodId, String name, String price) throws Exception {
        data(as("merchant", post("/api/foods/modifyItem").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("foodId", foodId, "foodName", name, "foodPrice", price)))));
        // 先证明菜单修改真的成功，防止“没改成功，所以旧订单没变”的假通过。
        var current = data(as("merchant", get("/api/foods/" + foodId)));
        assertEquals(name, current.path("foodName").asText());
        assertEquals(0, new BigDecimal(price).compareTo(current.path("foodPrice").decimalValue()));
    }

    private MockHttpServletRequestBuilder readRequest(long orderId, String view) {
        return switch (view) {
            case "merchant-list" -> get("/api/orders/list/business").param("businessId", "101");
            case "legacy" -> get("/api/foods").param("order", "" + orderId);
            default -> get("/api/orders/detail").param("orderId", "" + orderId);
        };
    }

    private JsonNode orderFoods(long orderId, String view) throws Exception {
        String actor = view.startsWith("merchant") ? "merchant" : "customer";
        JsonNode result = data(as(actor, readRequest(orderId, view)));
        if ("legacy".equals(view)) return result;
        if ("merchant-list".equals(view)) {
            for (JsonNode order : result) if (order.path("id").asLong() == orderId) return order.path("foodList");
            fail("商家订单列表中缺少订单 " + orderId);
        }
        return result.path("foodList");
    }

    private void assertItem(JsonNode items, long foodId, String name, String price, int quantity) {
        assertTrue(items.isArray(), "商品明细必须是数组");
        for (JsonNode item : items) {
            long id = item.has("foodId") ? item.path("foodId").asLong() : item.path("id").asLong();
            if (id != foodId) continue;
            assertAll(
                    () -> assertEquals(name, item.path("foodName").asText(), "订单商品名称"),
                    () -> assertEquals(0, new BigDecimal(price).compareTo(item.path("foodPrice").decimalValue()), "商品单价"),
                    () -> { if (item.has("quantity")) assertEquals(quantity, item.path("quantity").asInt(), "商品数量"); });
            return;
        }
        fail("订单明细中缺少商品 " + foodId);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
