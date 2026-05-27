package com.fishseedling.platform.util;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 加密工具类
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
@Component
public class EncryptUtil {

    /**
     * AES密钥（16位）
     */
    private static final String AES_KEY = "FishSeedling1234";

    /**
     * AES加密
     */
    public static String aesEncrypt(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        AES aes = SecureUtil.aes(AES_KEY.getBytes(StandardCharsets.UTF_8));
        return aes.encryptHex(content);
    }

    /**
     * AES解密
     */
    public static String aesDecrypt(String encryptedContent) {
        if (encryptedContent == null || encryptedContent.isEmpty()) {
            return encryptedContent;
        }
        try {
            AES aes = SecureUtil.aes(AES_KEY.getBytes(StandardCharsets.UTF_8));
            return aes.decryptStr(encryptedContent);
        } catch (Exception e) {
            return encryptedContent;
        }
    }

    /**
     * 电话号码脱敏显示
     * 例如：13800138000 -> 138****8000
     */
    public static String desensitizePhone(String phone) {
        return aesEncrypt(phone);
    }

    /**
     * MD5加密
     */
    public static String md5(String content) {
        return SecureUtil.md5(content);
    }
}

