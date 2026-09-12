package com.example.ridersafety.service;

import com.example.ridersafety.config.ApiException;
import com.example.ridersafety.model.Attachment;
import com.example.ridersafety.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/** 附件上传与读取：仅允许 JPG/PNG/GIF/WEBP 图片，单张最大 5MB，魔数校验防伪装 */
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepo;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public static final long MAX_SIZE = 5L * 1024 * 1024; // 5MB

    private static final Map<String, String> CONTENT_EXT = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp");

    /** 校验并保存上传的图片，返回附件记录 */
    @Transactional
    public Attachment store(MultipartFile file, String username) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("文件为空，请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw ApiException.badRequest("文件大小超出限制（单张最大 5MB）");
        }
        String contentType = file.getContentType();
        String ext = contentType != null ? CONTENT_EXT.get(contentType.toLowerCase()) : null;
        if (ext == null) {
            throw ApiException.badRequest("仅支持 JPG / PNG / GIF / WEBP 图片，当前类型: "
                    + (contentType != null ? contentType : "未知"));
        }
        byte[] head;
        try (InputStream in = file.getInputStream()) {
            head = in.readNBytes(12);
        } catch (IOException e) {
            throw ApiException.badRequest("读取文件失败: " + e.getMessage());
        }
        if (!magicMatches(head, contentType.toLowerCase())) {
            throw ApiException.badRequest("文件内容与声明的图片类型不符，已拒绝（疑似伪装文件）");
        }
        String storedName = UUID.randomUUID() + ext;
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(storedName));
        } catch (IOException e) {
            throw ApiException.badRequest("保存文件失败: " + e.getMessage());
        }
        Attachment a = new Attachment();
        a.setOriginalName(file.getOriginalFilename() != null ? file.getOriginalFilename() : storedName);
        a.setStoredName(storedName);
        a.setContentType(contentType.toLowerCase());
        a.setSize(file.getSize());
        a.setUploadedBy(username);
        return attachmentRepo.save(a);
    }

    @Transactional(readOnly = true)
    public Attachment get(Long id) {
        return attachmentRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("附件不存在"));
    }

    public Path pathOf(Attachment a) {
        return Paths.get(uploadDir).toAbsolutePath().normalize().resolve(a.getStoredName());
    }

    public Map<String, Object> toMap(Attachment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("name", a.getOriginalName());
        m.put("size", a.getSize());
        m.put("contentType", a.getContentType());
        m.put("url", "/api/attachments/" + a.getId());
        m.put("createdAt", a.getCreatedAt());
        return m;
    }

    /** 把逗号分隔的附件 ID 串解析为可访问的照片记录列表 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> photosOf(String photoIds) {
        List<Long> ids = parseIds(photoIds);
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Attachment> byId = new HashMap<>();
        attachmentRepo.findAllById(ids).forEach(a -> byId.put(a.getId(), a));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Long id : ids) {
            Attachment a = byId.get(id);
            if (a != null) {
                result.add(toMap(a));
            }
        }
        return result;
    }

    public static List<Long> parseIds(String photoIds) {
        if (photoIds == null || photoIds.isBlank()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String s : photoIds.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) {
                ids.add(Long.parseLong(t));
            }
        }
        return ids;
    }

    public static String joinIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Long id : ids) {
            if (id == null) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(id);
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /** 文件头魔数校验：防止把非图片文件改名伪装成图片 */
    private boolean magicMatches(byte[] b, String contentType) {
        if (b.length < 4) {
            return false;
        }
        int b0 = b[0] & 0xFF, b1 = b[1] & 0xFF, b2 = b[2] & 0xFF, b3 = b[3] & 0xFF;
        if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) {
            return "image/jpeg".equals(contentType);
        }
        if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) {
            return "image/png".equals(contentType);
        }
        if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46 && b3 == 0x38) {
            return "image/gif".equals(contentType);
        }
        if (b.length >= 12 && b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46
                && b[8] == 0x57 && b[9] == 0x45 && b[10] == 0x42 && b[11] == 0x50) {
            return "image/webp".equals(contentType);
        }
        return false;
    }
}
