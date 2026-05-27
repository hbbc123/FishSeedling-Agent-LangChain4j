package com.fishseedling.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 鱼苗供需信息发布平台 - 启动类
 *
 * @author 賀暢
 * @since 2026-5-27
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class  // 排除数据源自动配置
})
@MapperScan("com.fishseedling.platform.mapper")
public class FishSeedlingPlatformApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(FishSeedlingPlatformApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("鱼苗供需信息发布平台启动成功！");
        System.out.println("访问地址：http://localhost:8080/api");
        System.out.println("========================================\n");
//        StartCanalConsumer.build(context);
    }
}

