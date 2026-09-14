package com.tju.elm_bk.adapter;

public interface SpeechAdapter {
    SpeechTranscriptionResult transcribe(byte[] content, String mimeType);

    boolean isAvailable();

    String provider();

    record SpeechTranscriptionResult(String transcript, String language) {
    }
}
