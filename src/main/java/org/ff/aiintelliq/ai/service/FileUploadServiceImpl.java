package org.ff.aiintelliq.ai.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    @Resource
    private MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    /**
     * 单文件上传
     */
    @Override
    public String fileUpload(MultipartFile multipartFile) {
        try {
            // 确保存储桶存在
            ensureBucketExists();
            // 生成唯一文件名
            String originalFilename = multipartFile.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + suffix;

            // 上传文件
            try (InputStream inputStream = multipartFile.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fileName)
                                .stream(inputStream, multipartFile.getSize(), -1)
                                .contentType(multipartFile.getContentType())
                                .build()
                );
            }

            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 多文件上传
     */
    @Override
    public List<String> multiFileUpload(MultipartFile[] files) {
        List<String> fileNames = new ArrayList<>();

        // 检查文件数组是否为空
        if (files == null || files.length == 0) {
            throw new RuntimeException("请选择要上传的文件");
        }
        // 确保存储桶存在
        ensureBucketExists();
        // 遍历上传每个文件
        for (MultipartFile file : files) {
            // 跳过空文件
            if (file.isEmpty()) {
                continue;
            }

            try {
                String originalFilename = file.getOriginalFilename();
                String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
                String fileName = UUID.randomUUID().toString() + suffix;

                try (InputStream inputStream = file.getInputStream()) {
                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(fileName)
                                    .stream(inputStream, file.getSize(), -1)
                                    .contentType(file.getContentType())
                                    .build()
                    );
                }
                fileNames.add(fileName);
            } catch (Exception e) {
                throw new RuntimeException("文件 " + file.getOriginalFilename() + " 上传失败：" + e.getMessage());
            }
        }
        return fileNames;
    }

    /**
     * 确保存储桶存在，如果不存在则创建
     */
    public void ensureBucketExists() {
        try {
            // 检查存储桶是否存在
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            // 如果不存在则创建存储桶
            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
                 log.info("存储桶 {} 不存在，已自动创建", bucketName);
            } else {
                log.info("存储桶 {} 已存在", bucketName);
            }
        } catch (Exception e) {
            throw new RuntimeException("存储桶操作失败（桶名：" + bucketName + "）：" + e.getMessage(), e);
        }
    }


    @Override
    public void getFileByUserId() {
        // 实现根据用户ID获取文件的逻辑
    }
}
