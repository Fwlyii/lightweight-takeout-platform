package com.tju.elm_bk.adapter;

import java.util.List;

public interface ImageRecognitionAdapter {
    ImageRecognitionResult recognize(byte[] content, String mimeType);

    boolean isAvailable();

    String provider();

    record ImageRecognitionResult(String summary, List<String> keywords, double confidence) {
    }
}
