package com.fishseedling.platform.myLangChain4j.milvus.config;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.IngestionResult;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 向向量库中添加api内容和元信息
 *
 * <p>
 * 功能描述：TODO
 * 使用说明：TODO
 * </p>
 *
 * @author 贺畅
 * @version 1.0.0
 * @since 2026-05-27
 */
public class AddApiInfoConfig {

    public static void main(String[] args) {

        MilvusEmbeddingStore milvusEmbeddingStore = MilvusEmbeddingStore.builder()
                .collectionName("apiEmbeddingStore")
                .host("localhost")
                .port(19530)
                .dimension(1024)
                .username("root")
                .password("Milvus")
                .build();

        OllamaEmbeddingModel  ollamaEmbeddingModel= OllamaEmbeddingModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("qwen3-embedding:0.6b")
                .build();


        Document document1 = Document.from(logisticsListInfo, Metadata.from(JSONUtil.toBean(logisticsListMetadata, Map.class)));
        Document document2 = Document.from(logisticsInfo, Metadata.from(JSONUtil.toBean(logisticsMetadata, Map.class)));


        Document document3 = Document.from(fishListInfo, Metadata.from(JSONUtil.toBean(fishListMetadata, Map.class)));
        Document document4 = Document.from(fishInfo, Metadata.from(JSONUtil.toBean(fishMetadata, Map.class)));

        List<Document> documentList = List.of(document1, document2, document3, document4);

        EmbeddingStoreIngestor embeddingStoreIngestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(400, 20))
                .embeddingModel(ollamaEmbeddingModel)
                .embeddingStore(milvusEmbeddingStore)
                .build();
        IngestionResult ingest = embeddingStoreIngestor.ingest(documentList);


    }

    public static String logisticsListInfo= "搜索查询物流车辆服务列表，支持按车辆类型、地区、载重、价格、服务范围筛选，支持关键词搜索司机姓名和车牌号，返回多条物流车辆信息。参数：current当前页码、size每页条数、vehicleType车辆类型(冷藏车/水罐车/普通货车)、province省份、city城市、district区县、serviceScope服务范围(市内/省内/跨省)、hasRefrigeration是否有冷藏设备、hasOxygenSystem是否有增氧设备、minLoad最小载重量(吨)、maxLoad最大载重量(吨)、minPrice最低单价(元/公里)、maxPrice最高单价(元/公里)、keyword关键词(匹配车牌号/司机姓名/描述)、driverName司机姓名、plateNumber车牌号码、orderBy排序方式(createTime/viewCount/pricePerKm)";
    public static String logisticsListMetadata="{\"path\":\"/front/logistics/list\",\"interfaceId\":\"logistics_list_001\",\"method\":\"POST\",\"parameters\":\"{\\\"current\\\":\\\"当前页码，Long类型，非必填，默认1\\\",\\\"size\\\":\\\"每页条数，Long类型，必填5\\\",\\\"vehicleType\\\":\\\"车辆类型，String类型，非必填，可选值：冷藏车、水罐车、普通货车\\\",\\\"province\\\":\\\"省份，String类型，非必填\\\",\\\"city\\\":\\\"城市，String类型，非必填\\\",\\\"district\\\":\\\"区县，String类型，非必填\\\",\\\"serviceScope\\\":\\\"服务范围，String类型，非必填，可选值：市内、省内、跨省\\\",\\\"hasRefrigeration\\\":\\\"是否有冷藏设备，Boolean类型，非必填\\\",\\\"hasOxygenSystem\\\":\\\"是否有增氧设备，Boolean类型，非必填\\\",\\\"minLoad\\\":\\\"最小载重量，BigDecimal类型，非必填，单位吨\\\",\\\"maxLoad\\\":\\\"最大载重量，BigDecimal类型，非必填，单位吨\\\",\\\"minPrice\\\":\\\"最低单价，BigDecimal类型，非必填，单位元/公里\\\",\\\"maxPrice\\\":\\\"最高单价，BigDecimal类型，非必填，单位元/公里\\\",\\\"keyword\\\":\\\"关键词，String类型，非必填，可匹配车牌号/司机姓名/描述\\\",\\\"driverName\\\":\\\"司机姓名，String类型，非必填\\\",\\\"plateNumber\\\":\\\"车牌号码，String类型，非必填\\\",\\\"orderBy\\\":\\\"排序方式，String类型，非必填，可选值：createTime、viewCount、pricePerKm\\\"}\"}";

    public static String logisticsInfo="根据物流ID查看单条物流车辆详细信息，需要先知道物流ID，返回单条物流的完整信息包括车辆型号、载重、服务范围、司机联系方式等。参数：id物流信息ID(必填，位于路径中)\n";
    public static String logisticsMetadata="{\"interfaceId\":\"logistics_detail_001\",\"path\":\"/front/logistics/detail/{id}\",\"method\":\"GET\",\"parameters\":\"{\\\"id\\\":\\\"物流信息ID，Long类型，必填，位于路径中\\\"}\"}\n";

    public static String fishListInfo="搜索查询鱼苗供需信息列表，支持按信息类型、鱼苗品种、地区、价格筛选，支持关键词搜索和联系人搜索，返回多条供需信息。参数：current当前页码、size每页条数、type信息类型(supply供应/purchase求购)、fishBreed鱼苗品种、province省份(值必须加省或自治区)、city城市(值必须加市或自治州)、district区县(值必须加区或县或自治县)、minPrice最低价格、maxPrice最高价格、keyword关键词搜索、contactPerson联系人、orderBy排序方式、priceUnit价格单位(元/尾/元/斤/元/公斤/元/只)\n";
    public static String fishListMetadata="{\"interfaceId\":\"post_list_001\",\"path\":\"/front/post/list\",\"method\":\"POST\",\"parameters\":\"{\\\"current\\\":\\\"当前页码，Long类型，非必填，默认1\\\",\\\"size\\\":\\\"每页条数，Long类型，必填5\\\",\\\"type\\\":\\\"信息类型，String类型，非必填，可选值：supply或purchase\\\",\\\"fishBreed\\\":\\\"鱼苗品种，String类型，非必填\\\",\\\"province\\\":\\\"省份，String类型，非必填,值一定要加上省或自治区\\\",\\\"city\\\":\\\"城市，String类型，非必填,值一定要加上市或自治州\\\",\\\"district\\\":\\\"区县，String类型，非必填,值一定要加上区或县或自治县\\\",\\\"minPrice\\\":\\\"最低价格，BigDecimal类型，非必填\\\",\\\"maxPrice\\\":\\\"最高价格，BigDecimal类型，非必填\\\",\\\"keyword\\\":\\\"关键词搜索，String类型，非必填\\\",\\\"contactPerson\\\":\\\"联系人，String类型，非必填\\\",\\\"orderBy\\\":\\\"排序方式，String类型，非必填\\\",\\\"priceUnit\\\":\\\"价格单位，String类型，非必填，可选值：元/尾、元/斤、元/公斤、元/只\\\"}\"}\n";



    public static String fishInfo="根据信息ID查看单条鱼苗供需详细信息，需要先知道信息ID，返回单条信息的完整内容包括鱼苗品种、规格、数量、价格、地区、联系人、描述等。参数：id信息ID(必填，位于路径中)\n";
    public static String fishMetadata="{\"interfaceId\":\"post_detail_001\",\"path\":\"/front/post/detail/{id}\",\"method\":\"GET\",\"parameters\":\"{\\\"id\\\":\\\"信息ID，Long类型，必填，位于路径中\\\"}\"}\n";

}
