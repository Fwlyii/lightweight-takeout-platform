package com.tju.elm_bk.adapter.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmapUriMapAdapterTest {
    @Test
    void shouldEncodeAddressAndUseFixedAmapHost() {
        String url = new AmapUriMapAdapter().navigationUrl("天津市 南开区卫津路92号");

        assertTrue(url.startsWith("https://uri.amap.com/search?"));
        assertTrue(url.contains("keyword="));
        assertFalse(url.contains("卫津路"));
        assertFalse(url.contains(" "));
    }
}
