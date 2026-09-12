package com.example.ridersafety.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 上传的附件（事故照片 / 磨损照片） */
@Entity
@Table(name = "attachments")
@Getter
@Setter
@NoArgsConstructor
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 原始文件名 */
    @Column(nullable = false)
    private String originalName;

    /** 磁盘存储名（UUID + 扩展名） */
    @Column(nullable = false, unique = true)
    private String storedName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    private String uploadedBy;

    private LocalDateTime createdAt = LocalDateTime.now();
}
