package com.sky.utils;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 唯一文件名工具类
 */
public class UniqFileNameUtil {

    /**
     * 用来获取文件类型名称，并用一个唯一的UUID作为文件名
     * @param file 前端传来的文件
     * @return
     */
    public static String getFileName(MultipartFile file) {
        // 获取原始文件名
        String originFileName = file.getOriginalFilename();

        if (originFileName == null || !originFileName.contains(".")) {
            throw new IllegalArgumentException("上传文件名不合法，无法提取后缀");
        }

        String extension = originFileName.substring(originFileName.lastIndexOf('.'));
        return UUID.randomUUID() + extension;   // 使用 UUID 生成一个唯一文件名
    }
}
