package com.tju.elm_bk.adapter.impl;

import com.tju.elm_bk.adapter.SpeechAdapter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("ai-mock")
public class MockSpeechAdapter implements SpeechAdapter {
    @Override
    public SpeechTranscriptionResult transcribe(byte[] content, String mimeType) {
        return new SpeechTranscriptionResult("帮我来两份大杯少冰奶茶，预算30元", "zh-CN");
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
