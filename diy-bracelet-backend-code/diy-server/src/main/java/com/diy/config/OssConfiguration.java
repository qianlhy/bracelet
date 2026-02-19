package com.diy.config;

import com.diy.properties.WxCloudStorageProperties;
import com.diy.utils.WxCloudStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 对象存储配置类
 */
@Configuration
@Slf4j
public class OssConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public WxCloudStorageUtil wxCloudStorageUtil(WxCloudStorageProperties properties){
        log.info("开始创建微信云托管对象存储工具类对象: bucketName={}, region={}",
                properties.getBucketName(), properties.getRegion());
        return new WxCloudStorageUtil(
                properties.getBucketName(),
                properties.getRegion()
        );
    }
}
