package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.adapter.RecommendationAdapter;
import com.tju.elm_bk.mapper.PreferenceMapper;
import com.tju.elm_bk.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiRecommendationServiceImplTest {
    @Test
    void shouldExtractBudgetFromNaturalLanguage() {
        RecommendationAdapter adapter = mock(RecommendationAdapter.class);
        when(adapter.recommend(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        AiRecommendationServiceImpl service = new AiRecommendationServiceImpl(
                adapter, mock(PreferenceMapper.class), mock(UserMapper.class));

        service.recommend("想吃30元以内的牛肉面", null, false);

        ArgumentCaptor<RecommendationAdapter.RecommendationContext> captor =
                ArgumentCaptor.forClass(RecommendationAdapter.RecommendationContext.class);
        verify(adapter).recommend(captor.capture());
        assertEquals(new BigDecimal("30"), captor.getValue().budget());
        assertEquals(1, captor.getValue().quantity());
    }
}
