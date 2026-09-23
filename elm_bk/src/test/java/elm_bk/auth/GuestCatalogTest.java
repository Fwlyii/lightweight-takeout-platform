package elm_bk.auth;

import elm_bk.service.BusinessService;
import elm_bk.service.FoodService;
import elm_bk.service.ReviewService;
import elm_bk.vo.BusinessSearchVO;
import elm_bk.vo.BusinessVO;
import elm_bk.vo.ReviewVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:guest-catalog;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
@ActiveProfiles("auth-test")
class GuestCatalogTest {
    @Autowired MockMvc mvc;
    @MockBean BusinessService businesses;
    @MockBean FoodService foods;
    @MockBean ReviewService reviews;

    @Test
    void guestsCanReadCatalogButNotPrivateOrManagementEndpoints() throws Exception {
        BusinessVO business = new BusinessVO(); business.setStatus(1);
        when(businesses.getBusinessById(7L)).thenReturn(business);
        when(foods.getFoodItemList(7L, null)).thenReturn(List.of());
        for (String path : List.of("/api/businesses/search", "/api/businesses/type/presentations?type=1",
                "/api/businesses/carousel", "/api/businesses/public/7", "/api/foods/list?businessId=7",
                "/api/v1/reviews/public/business/7")) {
            mvc.perform(get(path)).andExpect(status().isOk());
        }
        for (String path : List.of("/api/user", "/api/carts", "/api/addresses", "/api/orders",
                "/api/businesses", "/api/businesses/7", "/api/businesses/active", "/api/businesses/merchant",
                "/api/businesses/type", "/api/foods", "/api/foods/status?foodId=1&shelveStatus=0",
                "/api/foods/delete?foodId=1", "/api/v1/reviews/order/1", "/api/v1/assets")) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/businesses")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/carts")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/businesses/7")).andExpect(status().isUnauthorized());
    }

    @Test
    void categoryUsesHomepagePresentationAndFiltersRequestedType() throws Exception {
        BusinessSearchVO meal = new BusinessSearchVO();
        meal.setId(7L); meal.setOrderTypeId(1); meal.setBusinessName("测试商家");
        BusinessSearchVO drink = new BusinessSearchVO();
        drink.setId(8L); drink.setOrderTypeId(2);
        when(businesses.getBusinessesBySearch(null, false, false)).thenReturn(List.of(meal, drink));
        mvc.perform(get("/api/businesses/type/presentations?type=1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(7));
        mvc.perform(get("/api/businesses/type/presentations?type=99"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void publicReviewsOmitPrivateIdentifiersAndHiddenContent() throws Exception {
        BusinessVO business = new BusinessVO(); business.setStatus(1);
        when(businesses.getBusinessById(7L)).thenReturn(business);
        ReviewVO visible = new ReviewVO();
        visible.setId(1L); visible.setOrderId(2L); visible.setCustomerId(3L);
        visible.setCustomerName("private-account"); visible.setContent("好吃");
        ReviewVO hidden = new ReviewVO(); hidden.setHidden(true); hidden.setContent("hidden");
        when(reviews.listByBusiness(7L)).thenReturn(List.of(visible, hidden));
        mvc.perform(get("/api/v1/reviews/public/business/7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].customerName").value("匿名顾客"))
                .andExpect(jsonPath("$.data[0].orderId").doesNotExist())
                .andExpect(jsonPath("$.data[0].customerId").doesNotExist());
    }

    @Test
    void storefrontOmitsOwnerAndAuditFields() throws Exception {
        BusinessVO business = new BusinessVO(); business.setStatus(1); business.setCreator(42L);
        business.setBusinessOwner(new elm_bk.vo.UserVO());
        when(businesses.getBusinessById(7L)).thenReturn(business);
        mvc.perform(get("/api/businesses/public/7")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.businessOwner").doesNotExist())
                .andExpect(jsonPath("$.data.creator").doesNotExist());
    }

    @Test
    void invalidTokenDoesNotPreventPublicBrowsingOrAuthorizePrivateAccess() throws Exception {
        mvc.perform(get("/api/businesses/search").header("Authorization", "Bearer expired"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/user").header("Authorization", "Bearer expired"))
                .andExpect(status().isUnauthorized());
    }
}
