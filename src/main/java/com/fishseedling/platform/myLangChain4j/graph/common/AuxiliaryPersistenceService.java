package com.fishseedling.platform.myLangChain4j.graph.common;


import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.fishseedling.platform.myLangChain4j.postgresql.mapper.PostgresAutomaticChatMessageStore;
import com.fishseedling.platform.myLangChain4j.postgresql.mapper.PostgresChatMessageStore;
import com.fishseedling.platform.myLangChain4j.postgresql.mapper.PostgresCheckpointStore;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;

@Component
public class AuxiliaryPersistenceService {
    @Resource
    private DynamicRoutingDataSource dynamicRoutingDataSource;

    @Bean
    public PostgresCheckpointStore buildPostgresCheckpointStore(){
        try {
            return  PostgresCheckpointStore.builder().datasource(dynamicRoutingDataSource.getDataSource("postgresql")).createTables(true).build();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    @Bean
    public PostgresChatMessageStore buildPostgresChatMessageStore (){
       return PostgresChatMessageStore.builder().dataSource(dynamicRoutingDataSource.getDataSource("postgresql")).build();
    }
    @Bean
    public PostgresAutomaticChatMessageStore buildPostgresAutomaticChatMessageStore (){
        return PostgresAutomaticChatMessageStore.builder().dataSource(dynamicRoutingDataSource.getDataSource("postgresql")).build();
    }
}
