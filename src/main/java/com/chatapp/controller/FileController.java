package com.chatapp.controller;

import com.chatapp.dto.response.ApiResponse;
import com.chatapp.model.FileAttachment;
import com.chatapp.security.UserPrincipal;
import com.chatapp.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController @RequestMapping("/api/files") @RequiredArgsConstructor
public class FileController {
    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse> upload(@AuthenticationPrincipal UserPrincipal p, @RequestParam("file") MultipartFile file) {
        FileAttachment a = fileStorageService.storeFile(file, p.getId());
        Map<String, Object> r = new HashMap<>();
        r.put("fileUrl", "/uploads/" + a.getStoredName()); r.put("fileName", a.getOriginalName());
        r.put("fileSize", a.getFileSize()); r.put("contentType", a.getContentType());
        return ResponseEntity.ok(ApiResponse.success("File uploaded", r));
    }
}
