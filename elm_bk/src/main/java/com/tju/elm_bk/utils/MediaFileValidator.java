package com.tju.elm_bk.utils;

import com.tju.elm_bk.exception.APIException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Component
public class MediaFileValidator {
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> AUDIO_TYPES = Set.of(
            "audio/webm", "audio/wav", "audio/x-wav", "audio/mpeg", "audio/mp3",
            "audio/mp4", "audio/x-m4a", "audio/ogg", "video/webm", "video/mp4");

    public ValidatedMedia image(MultipartFile file, long maxBytes) {
        ValidatedMedia media = read(file, maxBytes, "图片");
        if (!IMAGE_TYPES.contains(media.mimeType()) || !hasImageSignature(media.content(), media.mimeType())) {
            throw new APIException("只支持真实的JPG、PNG或WebP图片");
        }
        return media;
    }

    public ValidatedMedia audio(MultipartFile file, long maxBytes) {
        ValidatedMedia media = read(file, maxBytes, "音频");
        if (!AUDIO_TYPES.contains(media.mimeType()) || !hasAudioSignature(media.content(), media.mimeType())) {
            throw new APIException("只支持WebM、WAV、MP3、M4A、MP4或OGG音频");
        }
        return media;
    }

    private ValidatedMedia read(MultipartFile file, long maxBytes, String label) {
        if (file == null || file.isEmpty()) throw new APIException("请选择" + label + "文件");
        if (file.getSize() > maxBytes) throw new APIException(label + "文件过大");
        String mimeType = normalizeMimeType(file.getContentType());
        try {
            return new ValidatedMedia(file.getBytes(), mimeType);
        } catch (IOException ex) {
            throw new APIException(label + "读取失败");
        }
    }

    private String normalizeMimeType(String value) {
        if (value == null) return "application/octet-stream";
        return value.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasImageSignature(byte[] bytes, String mimeType) {
        return switch (mimeType) {
            case "image/jpeg" -> startsWith(bytes, 0xff, 0xd8, 0xff);
            case "image/png" -> startsWith(bytes, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a);
            case "image/webp" -> asciiAt(bytes, 0, "RIFF") && asciiAt(bytes, 8, "WEBP");
            default -> false;
        };
    }

    private boolean hasAudioSignature(byte[] bytes, String mimeType) {
        return switch (mimeType) {
            case "audio/webm", "video/webm" -> startsWith(bytes, 0x1a, 0x45, 0xdf, 0xa3);
            case "audio/wav", "audio/x-wav" -> asciiAt(bytes, 0, "RIFF") && asciiAt(bytes, 8, "WAVE");
            case "audio/mpeg", "audio/mp3" -> asciiAt(bytes, 0, "ID3") ||
                    (bytes.length >= 2 && unsigned(bytes[0]) == 0xff && (unsigned(bytes[1]) & 0xe0) == 0xe0);
            case "audio/mp4", "audio/x-m4a", "video/mp4" -> asciiAt(bytes, 4, "ftyp");
            case "audio/ogg" -> asciiAt(bytes, 0, "OggS");
            default -> false;
        };
    }

    private boolean startsWith(byte[] bytes, int... prefix) {
        if (bytes.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (unsigned(bytes[i]) != prefix[i]) return false;
        }
        return true;
    }

    private boolean asciiAt(byte[] bytes, int offset, String text) {
        if (bytes.length < offset + text.length()) return false;
        for (int i = 0; i < text.length(); i++) {
            if (bytes[offset + i] != (byte) text.charAt(i)) return false;
        }
        return true;
    }

    private int unsigned(byte value) {
        return value & 0xff;
    }

    public record ValidatedMedia(byte[] content, String mimeType) {
    }
}
