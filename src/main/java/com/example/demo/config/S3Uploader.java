package com.example.demo.config;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Uploader {

    private final AmazonS3Client amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    public String bucket;

    public String upload(MultipartFile multipartFile, String dirName) throws IOException{
        File uploadFile = convert(multipartFile)          // 파일 변환이 안되면 에러
                .orElseThrow(() -> new IllegalArgumentException("error: MultipartFile -> File convert fail"));

        return upload(uploadFile,dirName);
    }

    //S3로 파일 업로드하기
    private String upload(File uploadFile, String dirName){

        String fileName = dirName + "/" + UUID.randomUUID() + uploadFile.getName();    // S3에 저장된 파일 이름
        String uploadImgUrl = putS3(uploadFile, fileName);       // S3로 업로드
        removeNewFile(uploadFile);

        return uploadImgUrl;
    }

    //S3로 업로드
    private String putS3(File uploadFile, String fileName){
        amazonS3Client.putObject(new PutObjectRequest(bucket, fileName, uploadFile)
                .withCannedAcl(CannedAccessControlList.PublicRead));
        return amazonS3Client.getUrl(bucket, fileName).toString();
    }


    //S3에서 파일 삭제
    public void deleteFromS3(String source){
        amazonS3Client.deleteObject(new DeleteObjectRequest(bucket, source));
    }

    // 로컬에 저장된 이미지 지우기
    private void removeNewFile(File targetFile){
        if(targetFile.delete()) {
            log.info("File delete success");
            return;
        }
        log.info("File delete fail");
    }




    private Optional<File> convert(MultipartFile file) throws IOException{
        File convertFile = new File(System.getProperty("user.dir") + "/" + file.getOriginalFilename());
        if(convertFile.createNewFile()) {
            try(FileOutputStream fos = new FileOutputStream(convertFile)){
                fos.write(file.getBytes());
            }
            return Optional.of(convertFile);
        }
        return Optional.empty();
    }


}

