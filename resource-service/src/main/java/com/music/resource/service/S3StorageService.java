package com.music.resource.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Service
public class S3StorageService {

    private final S3Client s3Client;

    public S3StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String store(String bucket, String path, byte[] data) {
        String key = buildKey(path);
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType("audio/mpeg")
                        .build(),
                RequestBody.fromBytes(data));
        return key;
    }

    public byte[] retrieve(String bucket, String key) {
        return s3Client
                .getObjectAsBytes(GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build())
                .asByteArray();
    }

    public void delete(String bucket, String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    public String move(String sourceBucket, String sourceKey, String targetBucket, String targetPath) {
        String targetKey = buildKey(targetPath);
        s3Client.copyObject(CopyObjectRequest.builder()
                .sourceBucket(sourceBucket)
                .sourceKey(sourceKey)
                .destinationBucket(targetBucket)
                .destinationKey(targetKey)
                .build());
        delete(sourceBucket, sourceKey);
        return targetKey;
    }

    private String buildKey(String path) {
        String normalized = path == null ? "" : path.replaceAll("^/+", "").replaceAll("/+$", "");
        String fileName = UUID.randomUUID() + ".mp3";
        return normalized.isEmpty() ? fileName : normalized + "/" + fileName;
    }
}
