package com.tju.elm_bk.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.tju.elm_bk.dto.AddressCreateDTO;
import com.tju.elm_bk.dto.AiChatRequestDTO;
import com.tju.elm_bk.dto.BusinessPermissionDTO;
import com.tju.elm_bk.dto.MerchantInteractionDTO;
import com.tju.elm_bk.entity.DeliveryAddress;
import com.tju.elm_bk.mapper.DeliveryAddressMapper;
import com.tju.elm_bk.service.AccountWriteLock;
import com.tju.elm_bk.service.CurrentUserService;
import com.tju.elm_bk.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestIdentityBoundaryTest {

    @Test
    void aiRequestCannotInjectAUserIdThroughJson() throws Exception {
        AiChatRequestDTO request = new ObjectMapper().readValue(
                "{\"message\":\"test\",\"chatType\":\"general\",\"userId\":999}",
                AiChatRequestDTO.class);

        assertNull(request.getUserId());
    }

    @Test
    void interactionRequestCannotInjectAUserIdThroughJson() throws Exception {
        MerchantInteractionDTO request = new ObjectMapper().readValue(
                "{\"userId\":999,\"merchantId\":1,\"liked\":true,\"collected\":false}",
                MerchantInteractionDTO.class);

        assertNull(request.getUserId());
        assertEquals(1L, request.getMerchantId());
    }

    @Test
    void shopApplicationCannotInjectAUserIdThroughJson() throws Exception {
        BusinessPermissionDTO request = new ObjectMapper().readValue(
                "{\"businessName\":\"test\",\"userId\":999}",
                BusinessPermissionDTO.class);

        assertNull(request.getUserId());
        assertEquals("test", request.getBusinessName());
    }

    @Test
    void addressWithoutClientIdentityBelongsToAuthenticatedUser() {
        DeliveryAddressMapper addressMapper = mock(DeliveryAddressMapper.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        AccountWriteLock writeLock = mock(AccountWriteLock.class);

        AddressServiceImpl service =
                new AddressServiceImpl(addressMapper, currentUserService, writeLock);

        when(writeLock.acquire()).thenReturn(7L);
        when(addressMapper.listDeliveryAddressByUserId(7L))
                .thenReturn(java.util.List.of());

        service.create(validAddress());

        ArgumentCaptor<DeliveryAddress> captor =
                ArgumentCaptor.forClass(DeliveryAddress.class);

        verify(addressMapper).insert(captor.capture());

        assertEquals(7L, captor.getValue().getUserId());
        assertEquals(7L, captor.getValue().getCreator());
        assertEquals(7L, captor.getValue().getUpdater());
    }

    @Test
    void addressRequestCannotInjectOwnershipThroughJson() {
        assertThrows(
                UnrecognizedPropertyException.class,
                () -> new ObjectMapper().readValue(
                        """
                        {
                          "contactName":"张同学",
                          "contactSex":1,
                          "contactTel":"13800138000",
                          "address":"天津大学北洋园校区",
                          "userId":999
                        }
                        """,
                        AddressCreateDTO.class)
        );
    }

    private AddressCreateDTO validAddress() {
        AddressCreateDTO dto = new AddressCreateDTO();
        dto.setContactName("张同学");
        dto.setContactSex(1);
        dto.setContactTel("13800138000");
        dto.setAddress("天津大学北洋园校区");
        return dto;
    }
}
