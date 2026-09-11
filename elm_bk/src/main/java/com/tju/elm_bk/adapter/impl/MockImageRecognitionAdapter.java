package com.tju.elm_bk.adapter.impl;

import com.tju.elm_bk.adapter.ImageRecognitionAdapter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("ai-mock")
public class MockImageRecognitionAdapter implements ImageRecognitionAdapter {
    @Override
    public ImageRecognitionResult recognize(byte[] content, String mimeType) {
        return new ImageRecognitionResult("AI Mock 模式：返回固定识别结果", List.of("牛肉面", "面食"), 1.0);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String provider() {
        return "LOCAL_AI_MOCK";
    }
}
