package com.chatapp.service;

import com.chatapp.exception.BadRequestException;
import com.chatapp.model.*;
import com.chatapp.repository.FileAttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service @Slf4j @RequiredArgsConstructor
public class FileStorageService {
    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final UserService userService;

    @PostConstruct
    public void init() {
        try { Files.createDirectories(Paths.get(uploadDir)); }
        catch (IOException e) { throw new RuntimeException("Could not create upload dir", e); }
    }

    public FileAttachment storeFile(MultipartFile file, Long userId) {
        String origName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown");
        if (origName.contains("..")) throw new BadRequestException("Invalid file name");
        String ext = ""; int dot = origName.lastIndexOf('.'); if (dot > 0) ext = origName.substring(dot);
        String storedName = UUID.randomUUID() + ext;
        try {
            Path target = Paths.get(uploadDir).resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            User user = userService.getUserById(userId);
            FileAttachment a = FileAttachment.builder().originalName(origName).storedName(storedName)
                .filePath(target.toString()).contentType(file.getContentType()).fileSize(file.getSize()).uploadedBy(user).build();
            return fileAttachmentRepository.save(a);
        } catch (IOException e) { throw new RuntimeException("Could not store file", e); }
    }
}
