package org.ff.aiintelliq.ai.controller;

import jakarta.annotation.Resource;
import org.ff.aiintelliq.ai.service.FileUploadService;
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
        return fileUploadService.fileUpload(multipartFile);
    }

    // 多个文件上传
    @PostMapping("/multiFileUpload")
    public List<String> multiFileUpload(@RequestParam("files") MultipartFile[] multipartFiles) {
        return fileUploadService.multiFileUpload(multipartFiles);
    }

}
