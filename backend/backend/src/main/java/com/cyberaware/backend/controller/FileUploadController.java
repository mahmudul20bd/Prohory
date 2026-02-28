package com.cyberaware.backend.controller;

import com.cyberaware.backend.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.cyberaware.backend.service.CloudinaryService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class FileUploadController {

    @Autowired
    private CloudinaryService cloudinaryService;

    // 🔥 এই API টি অ্যান্ড্রয়েড থেকে কল হবে
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadEvidence(@RequestParam("file") MultipartFile file) {
        try {
            String fileUrl = cloudinaryService.uploadFile(file);

            // JSON রেসপন্স তৈরি করা হচ্ছে {"fileUrl": "https://res.cloudinary.com/..."}
            Map<String, String> response = new HashMap<>();
            response.put("fileUrl", fileUrl);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to upload file");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}