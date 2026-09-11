package com.tju.elm_bk.utils;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class VoiceOrderParser {
    private static final Pattern ARABIC_QUANTITY = Pattern.compile("(\\d{1,2})\\s*(份|个|杯|碗|盒|盘)");
    private static final Pattern CHINESE_QUANTITY = Pattern.compile("([一二两三四五六七八九十])\\s*(份|个|杯|碗|盒|盘)");
    private static final Pattern PREFIX_BUDGET = Pattern.compile("(?:预算|不超过|最多|控制在|价格在?)\\s*[¥￥]?\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:元|块)?");
    private static final Pattern SUFFIX_BUDGET = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)\\s*(?:元|块)(?:以内|以下)");
    private static final List<String> SPECIFICATIONS = List.of(
            "超大杯", "大杯", "中杯", "小杯", "加辣", "特辣", "中辣", "微辣", "少辣", "不辣",
            "正常冰", "多冰", "加冰", "少冰", "去冰", "常温", "热饮", "无糖", "少糖", "半糖", "全糖",
            "不要葱", "不要香菜", "不要蒜", "加葱", "加香菜", "加蒜");
    private static final Map<String, Integer> CHINESE_NUMBERS = Map.ofEntries(
            Map.entry("一", 1), Map.entry("二", 2), Map.entry("两", 2), Map.entry("三", 3),
            Map.entry("四", 4), Map.entry("五", 5), Map.entry("六", 6), Map.entry("七", 7),
            Map.entry("八", 8), Map.entry("九", 9), Map.entry("十", 10));

    public ParsedVoiceOrder parse(String transcript) {
        String normalized = transcript == null ? "" : transcript.trim();
        int quantity = parseQuantity(normalized);
        BigDecimal budget = parseBudget(normalized);
        LinkedHashSet<String> selectedSpecifications = new LinkedHashSet<>();
        for (String specification : SPECIFICATIONS) {
            if (normalized.contains(specification)) selectedSpecifications.add(specification);
        }

        String query = normalized
                .replaceAll("[，。！？、,.!?]", " ")
                .replaceAll("(?:\\d{1,2}|[一二两三四五六七八九十])\\s*(?:份|个|杯|碗|盒|盘)", " ")
                .replaceAll("(?:预算|不超过|最多|控制在|价格在?)\\s*[¥￥]?\\s*\\d+(?:\\.\\d{1,2})?\\s*(?:元|块)?", " ")
                .replaceAll("\\d+(?:\\.\\d{1,2})?\\s*(?:元|块)(?:以内|以下)", " ");
        for (String specification : selectedSpecifications) query = query.replace(specification, " ");
        query = query
                .replaceAll("(?:麻烦|请|可以|能不能|帮我|给我|我要|我想要|我想吃|想吃|来点|来|点一?下|点)", " ")
                .replaceAll("(?:谢谢|吧|好吗|可以吗)", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (query.isEmpty()) query = normalized;
        return new ParsedVoiceOrder(query, quantity, String.join("、", selectedSpecifications), budget);
    }

    private int parseQuantity(String text) {
        Matcher arabic = ARABIC_QUANTITY.matcher(text);
        if (arabic.find()) return clamp(Integer.parseInt(arabic.group(1)));
        Matcher chinese = CHINESE_QUANTITY.matcher(text);
        if (chinese.find()) return CHINESE_NUMBERS.getOrDefault(chinese.group(1), 1);
        return 1;
    }

    private BigDecimal parseBudget(String text) {
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(PREFIX_BUDGET);
        patterns.add(SUFFIX_BUDGET);
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                BigDecimal value = new BigDecimal(matcher.group(1));
                if (value.compareTo(BigDecimal.ZERO) > 0 && value.compareTo(new BigDecimal("9999.99")) <= 0) {
                    return value;
                }
            }
        }
        return null;
    }

    private int clamp(int value) {
        return Math.max(1, Math.min(99, value));
    }

    public record ParsedVoiceOrder(String query, int quantity, String specification, BigDecimal budget) {
    }
}
