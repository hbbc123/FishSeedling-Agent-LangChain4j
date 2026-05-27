package com.fishseedling.platform.myLangChain4j.canal.config;


import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.impl.SimpleCanalConnector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

@Slf4j
@Configuration
public class CanalConfig {

    @Value("${canal.host:127.0.0.1}")
    private String host;

    @Value("${canal.port:11111}")
    private int port;

    @Value("${canal.destination:example}")
    private String destination;



    @Bean(destroyMethod = "disconnect")
    public CanalConnector canalConnector() {
        log.info("创建 CanalConnector Bean: host={}, port={}, destination={}, clientId={}",
                host, port, destination);

        // 直接使用 SimpleCanalConnector
        return new SimpleCanalConnector(
                new InetSocketAddress(host, port),
                "",
                "",
                destination
        );
    }
}