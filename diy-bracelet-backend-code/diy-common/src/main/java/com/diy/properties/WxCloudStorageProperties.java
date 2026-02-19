package com.diy.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信云托管对象存储配置属性
 */
@Component
@ConfigurationProperties(prefix = "sky.wxcloud.storage")
@Data
public class WxCloudStorageProperties {

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 地域
     */
    private String region;

}
