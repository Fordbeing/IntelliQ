package org.ff.aiintelliq.ai.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileUploadService {
    String fileUpload(MultipartFile multipartFile);

    List<String> multiFileUpload(MultipartFile[] files);

    void getFileByUserId();
}
