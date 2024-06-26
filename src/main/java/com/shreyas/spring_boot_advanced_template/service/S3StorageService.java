package com.shreyas.spring_boot_advanced_template.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import com.shreyas.spring_boot_advanced_template.AppConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class S3StorageService {
    private final String bucketName;
    private final AmazonS3 s3Client;

    public S3StorageService(AmazonS3 s3Client, AppConstant appConstant) {
        this.s3Client = s3Client;
        bucketName = appConstant.AWSS3BucketName();
    }

    public boolean saveFile(MultipartFile file) {
        String fileName = System.currentTimeMillis() + "-" + file.getName();
        s3Client.putObject(bucketName, fileName, convertMultipartFileToFile(file));
        return true;
    }

    public boolean saveFile(String path, String filename, MultipartFile file) {
        try {
            if (file.isEmpty())
                throw new IllegalArgumentException("File must not be empty");

            PutObjectResult result = s3Client.putObject(bucketName, path + filename, convertMultipartFileToFile(file));
            return result != null;
        } catch (AmazonS3Exception e) {
            log.error("Failed to upload file", e);
            throw new IllegalStateException("Failed to upload file to Amazon S3.", e);
        }
    }

    public boolean saveFile(String path, String filename, Optional<Map<String, String>> optionalMetaData, InputStream inputStream) {
        ObjectMetadata metadata = new ObjectMetadata();
        optionalMetaData.ifPresent(map -> {
            map.forEach(metadata::addUserMetadata);
        });
        try {
            s3Client.putObject(bucketName, path + filename, inputStream, metadata);
        } catch (AmazonS3Exception e) {
            log.error("Failed to upload file", e);
            throw new IllegalStateException("Failed to upload file to Amazon S3.", e);
        }
        return true;
    }

    public byte[] downloadFile(String fileName) {
        S3Object s3Object = s3Client.getObject(bucketName, fileName);
        try {
            return IOUtils.toByteArray(s3Object.getObjectContent());
        } catch (IOException e) {
            log.error("Failed to retrieve file from S3 Object{}", fileName, e);
            return null;
        }
    }

    public boolean deleteFile(String fileName) {
        try {
            s3Client.deleteObject(bucketName, fileName);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete file from S3 Object{}", fileName, e);
            return false;
        }
    }

    private File convertMultipartFileToFile(MultipartFile file) {
        File convFile = new File(file.getOriginalFilename());
        try {
            FileOutputStream fos = new FileOutputStream(convFile);
            fos.write(file.getBytes());
            fos.close();
        } catch (IOException e) {
            log.error("Failed to convert multipart file to file", e);
        }
        return convFile;
    }
}
