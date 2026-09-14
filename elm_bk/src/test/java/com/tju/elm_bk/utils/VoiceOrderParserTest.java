package com.tju.elm_bk.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VoiceOrderParserTest {
    private final VoiceOrderParser parser = new VoiceOrderParser();

    @Test
    void shouldExtractChineseQuantityAndFoodQuery() {
        VoiceOrderParser.ParsedVoiceOrder result = parser.parse("帮我来两份牛肉面，谢谢");

        assertEquals(2, result.quantity());
        assertEquals("牛肉面", result.query());
        assertNull(result.budget());
    }

    @Test
    void shouldExtractArabicQuantity() {
        VoiceOrderParser.ParsedVoiceOrder result = parser.parse("我要3杯奶茶");

        assertEquals(3, result.quantity());
        assertEquals("奶茶", result.query());
    }

    @Test
    void shouldExtractSpecificationsAndBudget() {
        VoiceOrderParser.ParsedVoiceOrder result = parser.parse("帮我来两杯大杯少冰奶茶，预算30元");

        assertEquals(2, result.quantity());
        assertEquals("奶茶", result.query());
        assertEquals("大杯、少冰", result.specification());
        assertEquals(new BigDecimal("30"), result.budget());
    }

    @Test
    void shouldUseOneWhenQuantityIsMissing() {
        VoiceOrderParser.ParsedVoiceOrder result = parser.parse("推荐宫保鸡丁");

        assertEquals(1, result.quantity());
        assertEquals("推荐宫保鸡丁", result.query());
    }

    @Test
    void shouldExtractSpeechNormalizedCurrencyBudget() {
        VoiceOrderParser.ParsedVoiceOrder result = parser.parse("我要两份牛肉面，预算¥30，不辣");

        assertEquals(2, result.quantity());
        assertEquals("牛肉面", result.query());
        assertEquals("不辣", result.specification());
        assertEquals(new BigDecimal("30"), result.budget());
    }
}
