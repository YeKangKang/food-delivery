package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 注册 Redis 客户端实例
 */
@Configuration
@Slf4j
public class RedisConfig {

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // 获取一个 RedisTemplate 对象，用于服务器与 Redis 数据库交互
        log.info("开始创建 Redis 模版对象...");
        RedisTemplate redisTemplate = new RedisTemplate();

        // 设置 Redis 的连接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // 设置 Redis 的 key 序列化器为字符串（默认使用 JDK 序列化器，会导致 key 以二进制形式存储）
        // 若不设置，key 将无法在 redis-cli 中通过明文查询（如 get city），但 Java 代码中仍可正常访问
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    }
}
