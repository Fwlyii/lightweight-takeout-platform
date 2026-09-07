package com.tju.elm_bk.service;

import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.result.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
@Slf4j
public class ImageStorageService {
    public static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private final Path root;

    public ImageStorageService(@Value("${app.upload.directory:./uploads}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }

    public String storeImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() > MAX_IMAGE_SIZE) {
            throw new APIException(ResultCodeEnum.PARAM_VERIFIED_FAILED.getCode(), "请选择5MB以内的图片");
        }
        byte[] bytes = file.getBytes();
        String extension = detectImageExtension(bytes);
        if (bytes.length > MAX_IMAGE_SIZE || extension == null) {
            throw new APIException(ResultCodeEnum.PARAM_VERIFIED_FAILED.getCode(), "仅支持 JPG、PNG 或 WebP 图片");
        }
        Files.createDirectories(root);
        String name = UUID.randomUUID() + extension;
        Files.write(root.resolve(name), bytes, StandardOpenOption.CREATE_NEW);
        return "/uploads/" + name;
    }

    public void discard(String url) {
        if (url == null || !url.startsWith("/uploads/")) return;
        Path file = root.resolve(url.substring("/uploads/".length())).normalize();
        if (!root.equals(file.getParent())) return;
        try {
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            log.warn("未能清理失败注册产生的图片", ex);
        }
    }

    static String detectImageExtension(byte[] b) {
        if (b.length >= 3 && (b[0] & 255) == 255 && (b[1] & 255) == 216 && (b[2] & 255) == 255) return ".jpg";
        if (b.length >= 8 && (b[0] & 255) == 137 && b[1] == 80 && b[2] == 78 && b[3] == 71
                && b[4] == 13 && b[5] == 10 && b[6] == 26 && b[7] == 10) return ".png";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return ".webp";
        return null;
    }
}
