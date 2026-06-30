package com.lz_insurance.storage.service;

import com.lz_insurance.core.exception.ErrorCode;
import com.lz_insurance.core.exception.InsuranceException;
import io.minio.*;
import io.minio.errors.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MinioService {

    @Value("${minio.object.bucket-name}")
    private String bucketName;

    @Value("${minio.object.presignedUrlExpiryMinutes}")
    private int presignedUrlExpiryMinutes;

    private final MinioClient minioClient;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @SneakyThrows
    public void uploadFile(MultipartFile multipartFile,  String folder) {
        log.info("multipart getOriginalFilename: {},  getInputStream: {}, getResource: {},  getName: {}", multipartFile.getOriginalFilename(), multipartFile.getInputStream(), multipartFile.getResource(), multipartFile.getName());
        createBucket(bucketName);
        var objectName = generateObjectName(folder, multipartFile.getOriginalFilename());
        minioClient.putObject(PutObjectArgs.builder()
                       .bucket(bucketName)
                       .object(objectName)
                       .stream(multipartFile.getInputStream(), multipartFile.getSize(), -1L)
                       .contentType(multipartFile.getContentType())
                       .build());
        log.info("{} is successfully uploaded as object: {} to bucket: {}", multipartFile.getOriginalFilename(), objectName, bucketName);
    }

    public InputStream downloadFile(String objectName) {
        GetObjectResponse stream = null;
        try {
            stream = minioClient.getObject(GetObjectArgs.builder().object(objectName).bucket(bucketName).build());
        } catch (InsufficientDataException | ServerException | XmlParserException
                 | InvalidResponseException | InternalException e) {
            throw new InsuranceException(ErrorCode.DOCUMENT_NOT_FOUND, e.toString());
        } catch (MinioException exception) {
            log.error("Invalid key: {}", exception.getMessage());
            throw new InsuranceException(ErrorCode.DOCUMENT_NOT_FOUND, "File not found: " + objectName);
        }
        log.info("stream: {} is successfully downloaded from bucket: {}", stream, bucketName);
        return stream;
    }

    @SneakyThrows
    public String getPresignedUrl(String objectName)  {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Http.Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(presignedUrlExpiryMinutes, TimeUnit.HOURS) // URL valid for 12 hour
                    .build());
        }catch (InsufficientDataException |
                ServerException | XmlParserException | InvalidResponseException |
                InternalException e) {
            throw new InsuranceException(ErrorCode.STORAGE_ERROR, e.toString());
        } catch (MinioException e) {
            log.error("Failed to generate presigned URL", e);
            throw new InsuranceException(ErrorCode.STORAGE_ERROR, "Failed to generate URL");
        }
    }

    @SneakyThrows
    public void createBucket(String bucketName) {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            log.info("Creating bucket: " + bucketName);
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        } else
            log.info("Bucket: {} already exists", bucketName);
    }

    private String generateObjectName(String folder, String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return folder + "/" + UUID.randomUUID() + extension;
    }
}
