package org.ff.aiintelliq.ai.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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
            return saveFileToMinio(multipartFile);
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

            // 先对文件进行向量化存储

            try {
                String objectName = saveFileToMinio(file);
                fileNames.add(objectName);
            } catch (Exception e) {
                throw new RuntimeException("文件 " + file.getOriginalFilename() + " 上传失败：" + e.getMessage(), e);
            }
        }
        return fileNames;
    }

    private @NotNull String saveFileToMinio(MultipartFile file) throws IOException, ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException, InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException {
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + suffix;

        int index = originalFilename.lastIndexOf(".");
        // 判断文件类型，区分存储路径
        String folder = originalFilename.substring(index + 1);

        // 存储路径： userId/document/uuid.xxx 或 userId/image/uuid.xxx
        String objectName = "userId" + "/" + folder + "/" + fileName;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );
        }
        return objectName;
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


    /**
     * 根据用户ID和目录获取所有文件路径
     * @param userId 用户ID
     * @param folder 目录名 (document / image)
     * @return 文件路径列表
     */
    @Override
    public List<String> getFileByUserId(String userId, String folder) {
        List<String> fileList = new ArrayList<>();
        try {
            String prefix = userId + "/" + folder + "/";

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)   // 按前缀查询
                            .recursive(true)  // 递归获取子目录
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                fileList.add(item.objectName());
            }
        } catch (Exception e) {
            throw new RuntimeException("获取文件列表失败：" + e.getMessage(), e);
        }
        return fileList;
    }

    /**
     * 获取某个目录下的具体文件
     * @param userId 用户ID
     * @param folder 目录名 (document / image)
     * @param fileName 文件名 (如 123.pdf)
     * @return 文件流
     */
    public InputStream getFileByName(String userId, String folder, String fileName) {
        try {
            String objectName = userId + "/" + folder + "/" + fileName;

            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("获取文件失败：" + e.getMessage(), e);
        }
    }
}
