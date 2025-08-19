package org.ff.aiintelliq.ai.controller;

import jakarta.annotation.Resource;
import org.ff.aiintelliq.ai.service.FileUploadService;
import org.jsoup.nodes.Document;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class FileUploadController {

    @Resource
    private FileUploadService fileUploadService;

    // 单个文件上传
    @PostMapping("/fileUpload")
    public String fileUpload(@RequestParam("file") MultipartFile multipartFile) {
        try {
            List<String> fileByUserId = fileUploadService.getFileByUserId("userId", "pdf");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return fileUploadService.fileUpload(multipartFile);
    }

    // 多个文件上传
    @PostMapping("/multiFileUpload")
    public List<String> multiFileUpload(@RequestParam("files") MultipartFile[] multipartFiles) {
        return fileUploadService.multiFileUpload(multipartFiles);
    }

}
