package com.tju.elm_bk.service;

import com.tju.elm_bk.controller.AdminRiderController;
import com.tju.elm_bk.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminDeliveryAvailabilityTest {
    @Test
    void absentDeliveryModuleReturnsExplicit503NotEmptySuccess() throws Exception {
        var beans = new DefaultListableBeanFactory();
        var controller = new AdminRiderController(mock(RiderService.class), beans.getBeanProvider(DeliveryService.class));
        var mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/v1/admin/delivery-exceptions"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("FEATURE_UNAVAILABLE"));
    }

    @Test
    void installedDeliveryModuleIsCalledWithoutChangingController() throws Exception {
        var beans = new DefaultListableBeanFactory();
        var delivery = mock(DeliveryService.class);
        when(delivery.listExceptions(0)).thenReturn(List.of());
        beans.registerSingleton("delivery", delivery);
        var controller = new AdminRiderController(mock(RiderService.class), beans.getBeanProvider(DeliveryService.class));
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(get("/api/v1/admin/delivery-exceptions").param("status", "0"))
                .andExpect(status().isOk());
        verify(delivery).listExceptions(0);
    }
}
