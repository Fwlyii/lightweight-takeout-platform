package com.tju.elm_bk.service;

import com.tju.elm_bk.dto.RecommendationRequestDTO;
import com.tju.elm_bk.vo.AiRecommendationVO;

import java.math.BigDecimal;
import java.util.List;

public interface AiRecommendationService {
    List<AiRecommendationVO> recommend(RecommendationRequestDTO request);

    List<AiRecommendationVO> recommend(String query, BigDecimal budget, boolean usePreferences);
}
