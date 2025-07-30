package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * AliOss 客户端对象工具类的配置类，用于创建一个封装好的用于文件上传的 阿里云OSS 工具对象
 */
@Configuration
@Slf4j
public class AliOssConfig {

    @Bean
    @ConditionalOnMissingBean   // 如果容器中没有这个类型/名字的 Bean，才注册当前这个 Bean
    public AliOssUtil getAliOssUtil(AliOssProperties aliOssProperties) {    // 注入自定的属性类bean

        log.info("创建阿里云OSS文件上传工具对象: {}", aliOssProperties);
        return new AliOssUtil(aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(),
                aliOssProperties.getBucketName());
    }
}
