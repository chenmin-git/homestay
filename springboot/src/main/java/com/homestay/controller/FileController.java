package com.homestay.controller;

import com.homestay.common.ApiResponse;
import com.homestay.common.BusinessException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/public")
public class FileController {

    private static final String UPLOAD_DIR = "uploads";

    @PostMapping("/upload")
    public ApiResponse<?> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        try {
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String filename = UUID.randomUUID().toString().replace("-", "") + extension;
            Path path = Paths.get(UPLOAD_DIR, filename);
            Files.write(path, file.getBytes());

            String url = "/uploads/" + filename;
            return ApiResponse.ok("上传成功", url);
        } catch (IOException e) {
            throw new BusinessException("文件保存失败: " + e.getMessage());
        }
    }
}
