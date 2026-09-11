package com.tju.elm_bk.utils;

import com.tju.elm_bk.exception.APIException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaFileValidatorTest {
    private final MediaFileValidator validator = new MediaFileValidator();

    @Test
    void shouldAcceptJpegSignature() {
        MockMultipartFile file = new MockMultipartFile("image", "dish.jpg", "image/jpeg",
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00});

        MediaFileValidator.ValidatedMedia result = validator.image(file, 1024);

        assertEquals("image/jpeg", result.mimeType());
    }

    @Test
    void shouldRejectSpoofedImage() {
        MockMultipartFile file = new MockMultipartFile("image", "dish.jpg", "image/jpeg", "not-an-image".getBytes());

        assertThrows(APIException.class, () -> validator.image(file, 1024));
    }

    @Test
    void shouldRejectOversizedImageBeforeSignatureUse() {
        MockMultipartFile file = new MockMultipartFile("image", "dish.jpg", "image/jpeg",
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00, 0x01});

        assertThrows(APIException.class, () -> validator.image(file, 4));
    }

    @Test
    void shouldAcceptWebmAudioWithCodecContentType() {
        MockMultipartFile file = new MockMultipartFile("audio", "voice.webm", "audio/webm;codecs=opus",
                new byte[]{0x1a, 0x45, (byte) 0xdf, (byte) 0xa3, 0x00});

        MediaFileValidator.ValidatedMedia result = validator.audio(file, 1024);

        assertEquals("audio/webm", result.mimeType());
    }

    @Test
    void shouldRejectOversizedAudio() {
        MockMultipartFile file = new MockMultipartFile("audio", "voice.webm", "audio/webm",
                new byte[]{0x1a, 0x45, (byte) 0xdf, (byte) 0xa3, 0x00});

        assertThrows(APIException.class, () -> validator.audio(file, 4));
    }
}
