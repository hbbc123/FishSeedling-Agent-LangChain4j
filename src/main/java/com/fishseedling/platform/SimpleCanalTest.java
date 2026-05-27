package com.fishseedling.platform;


import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import java.net.InetSocketAddress;

public class SimpleCanalTest {

    public static void main(String[] args) throws Exception {
        CanalConnector connector = CanalConnectors.newSingleConnector(
                new InetSocketAddress("127.0.0.1", 11111),
                "example",
                "",
                ""
        );

        connector.connect();
        connector.subscribe("fish_seedling_platform\\.tb_handbooks");
        connector.rollback();

        System.out.println("开始监听...");

        while (true) {
            Message message = connector.getWithoutAck(100, 5000L, java.util.concurrent.TimeUnit.MILLISECONDS);

            if (message.getId() != -1 && message.getEntries().size() > 0) {
                System.out.println("收到消息, batchId: " + message.getId());

                for (CanalEntry.Entry entry : message.getEntries()) {
                    System.out.println("Entry 类型: " + entry.getEntryType());

                    if (entry.getEntryType() == CanalEntry.EntryType.ROWDATA) {
                        CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                        System.out.println("事件类型: " + rowChange.getEventType());
                        System.out.println("行数据: " + rowChange.getRowDatasList());
                    }
                }

                connector.ack(message.getId());
            }

            Thread.sleep(100);
        }
    }
}