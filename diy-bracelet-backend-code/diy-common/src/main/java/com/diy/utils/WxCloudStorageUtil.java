package com.diy.utils;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicSessionCredentials;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

/**
 * 微信云托管对象存储工具类
 */
@Data
@AllArgsConstructor
@Slf4j
public class WxCloudStorageUtil {

    private String bucketName;
    private String region;
    
    // 微信云托管开放接口地址（仅容器内可访问）
    private static final String GET_AUTH_URL = "http://api.weixin.qq.com/_/cos/getauth";
    private static final String GET_METADATA_URL = "http://api.weixin.qq.com/_/cos/metaid/encode";
    
    /**
     * 临时密钥信息
     */
    private static class TempCredentials {
        String tmpSecretId;
        String tmpSecretKey;
        String token;
        Long expiredTime;
    }
    
    /**
     * 元数据响应
     */
    private static class MetadataResponse {
        int errcode;
        String errmsg;
        RespData respdata;
        
        static class RespData {
            List<String> x_cos_meta_field_strs;
        }
    }
    
    /**
     * 文件上传
     * 
     * @param bytes 文件字节数组
     * @param objectName 对象名称（文件路径）
     * @return 文件访问路径
     */
    public String upload(byte[] bytes, String objectName) {
        COSClient cosClient = null;
        try {
            // 1. 获取临时密钥
            TempCredentials credentials = getTempCredentials();
            
            // 2. 初始化COS客户端
            cosClient = initCOSClient(credentials);
            
            // 3. 获取文件元数据
            String metadata = getFileMetadata(objectName);
            
            // 4. 上传文件
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(bytes.length);
            // 设置元数据，保证小程序端可以访问
            if (metadata != null && !metadata.isEmpty()) {
                objectMetadata.setHeader("x-cos-meta-fileid", metadata);
            }
            
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                bucketName, 
                objectName, 
                new ByteArrayInputStream(bytes),
                objectMetadata
            );
            
            cosClient.putObject(putObjectRequest);
            
            // 5. 构建访问路径（通过后端图片读取接口）
            String fileUrl = "/admin/common/image/" + objectName;
            
            log.info("文件上传成功: {}", fileUrl);
            return fileUrl;
            
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        } finally {
            if (cosClient != null) {
                cosClient.shutdown();
            }
        }
    }
    
    /**
     * 获取临时密钥
     */
    private TempCredentials getTempCredentials() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                GET_AUTH_URL,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                
                TempCredentials credentials = new TempCredentials();
                credentials.tmpSecretId = (String) body.get("TmpSecretId");
                credentials.tmpSecretKey = (String) body.get("TmpSecretKey");
                credentials.token = (String) body.get("Token");
                credentials.expiredTime = Long.valueOf(body.get("ExpiredTime").toString());
                
                log.info("获取临时密钥成功");
                return credentials;
            } else {
                throw new RuntimeException("获取临时密钥失败");
            }
        } catch (Exception e) {
            log.error("获取临时密钥异常", e);
            throw new RuntimeException("获取临时密钥异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 初始化COS客户端
     */
    private COSClient initCOSClient(TempCredentials credentials) {
        BasicSessionCredentials cred = new BasicSessionCredentials(
            credentials.tmpSecretId,
            credentials.tmpSecretKey,
            credentials.token
        );
        
        Region regionObj = new Region(region);
        ClientConfig clientConfig = new ClientConfig(regionObj);
        
        return new COSClient(cred, clientConfig);
    }
    
    /**
     * 获取文件元数据
     * 用于小程序端访问控制
     */
    private String getFileMetadata(String objectPath) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("openid", ""); // 管理端上传，openid为空字符串
            requestBody.put("bucket", bucketName);
            requestBody.put("paths", Collections.singletonList("/" + objectPath));
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                GET_METADATA_URL,
                HttpMethod.POST,
                request,
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Integer errcode = (Integer) body.get("errcode");
                
                if (errcode != null && errcode == 0) {
                    Map<String, Object> respdata = (Map<String, Object>) body.get("respdata");
                    if (respdata != null) {
                        List<String> metadataList = (List<String>) respdata.get("x_cos_meta_field_strs");
                        if (metadataList != null && !metadataList.isEmpty()) {
                            log.info("获取文件元数据成功");
                            return metadataList.get(0);
                        }
                    }
                }
            }
            
            log.warn("获取文件元数据失败，继续上传");
            return null;
            
        } catch (Exception e) {
            log.warn("获取文件元数据异常，继续上传: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从对象存储下载文件
     * @param objectName 对象名称（文件路径）
     * @return 文件字节数组，未找到或出错时返回null
     */
    public byte[] download(String objectName) {
        COSClient cosClient = null;
        try {
            // 获取临时密钥并初始化COS客户端
            TempCredentials credentials = getTempCredentials();
            cosClient = initCOSClient(credentials);

            COSObject cosObject = cosClient.getObject(bucketName, objectName);
            if (cosObject == null) {
                log.warn("对象存储中未找到文件: {}", objectName);
                return null;
            }

            try (InputStream inputStream = cosObject.getObjectContent();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                byte[] data = outputStream.toByteArray();
                log.info("从对象存储下载文件成功: {}，大小={}字节", objectName, data.length);
                return data;
            }
        } catch (Exception e) {
            log.error("从对象存储下载文件失败: {}", objectName, e);
            return null;
        } finally {
            if (cosClient != null) {
                cosClient.shutdown();
            }
        }
    }
    
    /**
     * 从对象存储删除文件
     * @param fileUrlOrObjectName 文件访问路径或对象名称
     */
    public void delete(String fileUrlOrObjectName) {
        if (fileUrlOrObjectName == null || fileUrlOrObjectName.trim().isEmpty()) {
            return;
        }
        String objectName = fileUrlOrObjectName.trim();
        // 如果是通过后端图片读取接口返回的完整URL，去掉前缀
        String prefix = "/admin/common/image/";
        if (objectName.startsWith(prefix)) {
            objectName = objectName.substring(prefix.length());
        }
        // 去掉开头的斜杠，保持与上传时的objectName一致
        if (objectName.startsWith("/")) {
            objectName = objectName.substring(1);
        }
        COSClient cosClient = null;
        try {
            TempCredentials credentials = getTempCredentials();
            cosClient = initCOSClient(credentials);
            cosClient.deleteObject(bucketName, objectName);
            log.info("从对象存储删除文件成功: {}", objectName);
        } catch (Exception e) {
            log.error("从对象存储删除文件失败: {}", objectName, e);
        } finally {
            if (cosClient != null) {
                cosClient.shutdown();
            }
        }
    }
}
