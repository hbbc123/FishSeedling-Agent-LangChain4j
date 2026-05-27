package com.fishseedling.platform.util;

import java.util.Random;

/**
 * 随机数工具类
 *
 * @author Fish Seedling Platform
 * @since 2025-10-12
 */
public class RandomUtil {

    private static final Random RANDOM = new Random();
    private static final String NUMBERS = "0123456789";

    /**
     * 生成6位数字随机码
     */
    public static String generateManageCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(NUMBERS.charAt(RANDOM.nextInt(NUMBERS.length())));
        }
        return sb.toString();
    }

    /**
     * 生成指定长度的数字随机码
     */
    public static String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(NUMBERS.charAt(RANDOM.nextInt(NUMBERS.length())));
        }
        return sb.toString();
    }
}





