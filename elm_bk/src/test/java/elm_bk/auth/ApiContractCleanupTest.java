package elm_bk.auth;

import elm_bk.service.CartService;
import elm_bk.service.FoodService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:api-cleanup;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password="})
@AutoConfigureMockMvc
@ActiveProfiles("auth-test")
class ApiContractCleanupTest {
    @Autowired MockMvc mvc;
    @MockBean FoodService foods;
    @MockBean CartService carts;
    @MockBean elm_bk.service.OrderService orders;
    @MockBean elm_bk.service.MerchantInteractionService interactions;

    @Test @WithMockUser(authorities="BUSINESS")
    void merchantCanReadAggregateStatsButCannotUseCustomerInteractions() throws Exception {
        when(interactions.getMerchantStats(3L)).thenReturn(new elm_bk.vo.MerchantStatsVO());
        mvc.perform(get("/api/merchant/interaction/stats/3")).andExpect(status().isOk());
        mvc.perform(post("/api/merchant/interaction/update").contentType(MediaType.APPLICATION_JSON)
                .content("{\"merchantId\":3,\"liked\":true}"))
                .andExpect(status().isForbidden());
        verify(interactions).getMerchantStats(3L);
        verifyNoMoreInteractions(interactions);
    }

    @Test @WithMockUser(authorities="RIDER")
    void unrelatedPortalCannotReadStoreManagementCounters() throws Exception {
        mvc.perform(get("/api/merchant/interaction/stats/3")).andExpect(status().isForbidden());
        verifyNoInteractions(interactions);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"", " ", "1,", "1,,2", "1,-2", "0", "+1", "9999999999999999999999"})
    @WithMockUser(authorities="USER")
    void malformedSelectionCannotSilentlyChangeTheOrder(String foodIds) throws Exception {
        mvc.perform(post("/api/orders/submit").param("businessId","1").param("foodIds",foodIds))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(orders);
    }

    @Test @WithMockUser(authorities="BUSINESS")
    void merchandiseMutationsUseWriteMethods() throws Exception {
        when(foods.setFoodStatus(3L,0)).thenReturn(3L);
        when(foods.deleteFood(3L)).thenReturn(3L);
        mvc.perform(patch("/api/foods/3/status?shelveStatus=0")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(3));
        mvc.perform(delete("/api/foods/3")).andExpect(status().isOk());
        verify(foods).setFoodStatus(3L,0); verify(foods).deleteFood(3L);
    }

    @Test @WithMockUser(authorities="USER")
    void customerCannotModifyMerchandise() throws Exception {
        mvc.perform(patch("/api/foods/3/status?shelveStatus=0")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/foods/3")).andExpect(status().isForbidden());
        verifyNoInteractions(foods);
    }

    @Test @WithMockUser(authorities="USER")
    void retiredCartGetRequestsCannotMutateData() throws Exception {
        for (String endpoint : new String[]{"add?foodId=3&quantity=1", "quantity?cartId=1&quantity=2",
                "clear?businessId=1", "remove?cartId=1"}) {
            mvc.perform(get("/api/carts/"+endpoint)).andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("$.success").value(false));
        }
        verifyNoInteractions(carts);
    }

    @Test @WithMockUser(authorities="BUSINESS")
    void retiredFoodGetRequestsCannotMutateData() throws Exception {
        mvc.perform(get("/api/foods/status?foodId=3&shelveStatus=0")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/foods/delete?foodId=3")).andExpect(status().is4xxClientError());
        verifyNoInteractions(foods);
    }

    @Test @WithMockUser(authorities="USER")
    void cartCanBeClearedOnlyByDelete() throws Exception {
        when(carts.clearCart(7L)).thenReturn(7L);
        mvc.perform(delete("/api/carts?businessId=7")).andExpect(status().isOk());
        verify(carts).clearCart(7L);
    }

    @Test @WithMockUser
    void missingRoutesAndRemovedDemoEndpointsAre404Not500() throws Exception {
        for (String path : new String[]{"/api/no-such-endpoint", "/httpRest/success", "/httpRest/failure"}) {
            mvc.perform(get(path)).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    @Test @WithMockUser(authorities="USER")
    void malformedOrUnsupportedBodyKeepsClientErrorStatus() throws Exception {
        mvc.perform(post("/api/carts/items").contentType(MediaType.TEXT_PLAIN).content("bad"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(post("/api/carts/items").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(carts);
    }
}
