package com.example.ridersafety.controller;

import com.example.ridersafety.model.Attachment;
import com.example.ridersafety.service.AttachmentService;
import com.example.ridersafety.service.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final CurrentUser currentUser;

    /** 上传图片（multipart/form-data，字段名 file），返回附件记录（含可访问 URL） */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(Authentication auth, @RequestParam("file") MultipartFile file) {
        String username = currentUser.require(auth).getUsername();
        return attachmentService.toMap(attachmentService.store(file, username));
    }

    /** 读取附件内容（登录用户可访问，用于照片回显与复核） */
    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Attachment a = attachmentService.get(id);
        Path path = attachmentService.pathOf(a);
        Resource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename*=UTF-8''" + java.net.URLEncoder.encode(
                                a.getOriginalName(), java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"))
                .contentLength(a.getSize())
                .body(resource);
    }
}
