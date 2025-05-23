package edu.help.microservice.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;

@Service
public class S3Service {

    private static final String BUCKET = "megrimages";
    private static final Region REGION = Region.US_EAST_1;

    private final S3Presigner presigner;

    public S3Service() {
        this.presigner = S3Presigner.builder()
                .region(REGION)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /** Returns a presigned PUT URL that the frontend can use. */
    public PresignedPutObjectRequest generatePresignedUrl(String key) {

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build();                         // 👈  no Content-Type, no ACL

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(objectRequest)
                .signatureDuration(Duration.ofMinutes(10))
                .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
        System.out.println("Presigned URL: " + presigned.url());
        System.out.println("Signed headers: " + presigned.signedHeaders()); // should now be {host=[…]}
        return presigned;
    }
}
