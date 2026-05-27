package com.fishseedling.platform.myLangChain4j.postgresql.mapper;


// ============================================
// 包声明：LangGraph4j 框架的检查点模块
// ============================================

import com.fishseedling.platform.myLangChain4j.graph.common.UserSessionState;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ChatMessage;
import org.bsc.langgraph4j.RunnableConfig;              // 运行时配置类，包含线程ID等配置
import org.bsc.langgraph4j.checkpoint.AbstractCheckpointSaver;
import org.bsc.langgraph4j.checkpoint.Checkpoint;
import org.bsc.langgraph4j.langchain4j.serializer.std.ChatMesssageSerializer;
import org.bsc.langgraph4j.langchain4j.serializer.std.ToolExecutionRequestSerializer;
import org.bsc.langgraph4j.serializer.StateSerializer;   // 状态序列化器接口，用于序列化/反序列化状态
import org.bsc.langgraph4j.serializer.PlainTextStateSerializer; // 纯文本状态序列化器实现
import org.bsc.langgraph4j.serializer.std.ObjectStreamStateSerializer;
import org.bsc.langgraph4j.state.AgentState;             // Agent状态基类，所有状态的父类
import org.postgresql.ds.PGSimpleDataSource;             // PostgreSQL 简单数据源实现

import javax.sql.DataSource;                             // Java 标准数据源接口（连接池）
import java.io.IOException;                               // IO异常类
import java.nio.charset.StandardCharsets;                // 标准字符集（UTF-8）
import java.sql.*;                                        // JDBC 相关类（Connection, PreparedStatement等）
import java.util.*;                                       // Java 集合工具类
import lombok.extern.slf4j.Slf4j;

import static java.lang.String.format;                   // 字符串格式化静态导入
import static java.util.Objects.requireNonNull;          // 非空检查静态导入

/**
 * PostgreSQL 检查点保存器
 *
 * 将工作流的状态检查点保存到 PostgreSQL 数据库中
 * 支持事务、高并发、JSONB 数据类型
 *
 * @author LangGraph4j 团队
 */
@Slf4j
public class PostgresCheckpointStore extends AbstractCheckpointSaver {



    // ============================================
    // 构建器类（Builder Pattern）
    // 用于创建 PostgresSaver 实例，支持链式配置
    // ============================================

    public static class Builder {

        // 状态序列化器：负责将状态对象转换为字节/文本
        public StateSerializer<? extends AgentState> stateSerializer;

        // PostgreSQL 连接参数
        private String host;          // 数据库服务器地址
        private Integer port;         // 数据库端口（默认5432）
        private String user;          // 数据库用户名
        private String password;      // 数据库密码
        private String database;      // 数据库名称

        private boolean createTables;     // 是否自动创建表
        private boolean dropTablesFirst;  // 是否先删除已有表（清空数据）

        private DataSource datasource;    // 外部数据源（可选，优先使用）

        // 纯文本序列化器兼容模式标志
        // true：将 JSON 以文本形式存储（兼容旧版本）
        // false：以二进制形式存储（默认）
        private boolean plainTextStateSerializerLegacyMode = false;

        // 额外的数据库连接属性（如 ssl、timeout 等）
        private final Properties additionalProperties = new Properties();

        /**
         * 设置状态序列化器
         * @param stateSerializer 序列化器实例
         * @return 当前构建器（支持链式调用）
         */
        public <State extends AgentState> Builder stateSerializer(StateSerializer<State> stateSerializer) {
            this.stateSerializer = stateSerializer;
            return this;
        }

        /**
         * 设置纯文本序列化器兼容模式
         * 当状态序列化器是 PlainTextStateSerializer 时生效
         * 旧模式：将 JSON 保存为二进制格式（序列化的 Java String）
         *
         * @param mode 兼容模式标志（默认 false）
         * @return 当前构建器
         */
        public Builder plainTextStateSerializerLegacyMode( boolean mode ) {
            this.plainTextStateSerializerLegacyMode = mode;
            return this;
        }

        /**
         * 设置数据库主机地址
         * @param host 主机名或 IP 地址
         * @return 当前构建器
         */
        public Builder host(String host) {
            this.host = host;
            return this;
        }

        /**
         * 设置数据库端口
         * @param port 端口号（PostgreSQL 默认 5432）
         * @return 当前构建器
         */
        public Builder port(Integer port) {
            this.port = port;
            return this;
        }

        /**
         * 设置数据库用户名
         * @param user 用户名
         * @return 当前构建器
         */
        public Builder user(String user) {
            this.user = user;
            return this;
        }

        /**
         * 设置数据库密码
         * @param password 密码
         * @return 当前构建器
         */
        public Builder password(String password) {
            this.password = password;
            return this;
        }

        /**
         * 设置数据库名称
         * @param database 数据库名
         * @return 当前构建器
         */
        public Builder database(String database) {
            this.database = database;
            return this;
        }

        /**
         * 设置外部数据源（推荐使用连接池）
         * 如果设置了数据源，则忽略 host/port/user/password 等参数
         *
         * @param datasource 数据源（如 HikariCP、Druid）
         * @return 当前构建器
         */
        public Builder datasource(DataSource datasource){
            this.datasource = datasource;
            return this;
        }

        /**
         * 添加单个额外连接属性
         * @param name 属性名（如 "ssl"、"connectTimeout"）
         * @param value 属性值
         * @return 当前构建器
         */
        public Builder property(String name, String value) {
            this.additionalProperties.setProperty(name, value);
            return this;
        }

        /**
         * 批量添加额外连接属性
         * @param properties 属性集合
         * @return 当前构建器
         */
        public Builder properties(Properties properties) {
            this.additionalProperties.putAll(properties);
            return this;
        }

        /**
         * 设置是否自动创建表
         * @param createTables true=自动创建表，false=不创建（需要手动建表）
         * @return 当前构建器
         */
        public Builder createTables(boolean createTables) {
            this.createTables = createTables;
            return this;
        }

        /**
         * 设置是否先删除表再创建
         * @param dropTablesFirst true=删除已有表（清空所有检查点数据），false=保留
         * @return 当前构建器
         */
        public Builder dropTablesFirst(boolean dropTablesFirst) {
            this.dropTablesFirst = dropTablesFirst;
            return this;
        }

        /**
         * 检查字符串参数不为空且不为空白
         * @param value 待检查的字符串
         * @param name 参数名称（用于错误信息）
         * @return 原字符串（如果有效）
         * @throws IllegalArgumentException 如果字符串为 null 或空白
         */
        private String requireNotBlank( String value, String name ) {
            if( requireNonNull(value, format("'%s' cannot be null", name) ).isBlank() ) {
                throw new IllegalArgumentException(format("'%s' cannot be blank", name));
            }
            return value;
        }

        /**
         * 构建 PostgresSaver 实例
         * 优先使用外部数据源，否则根据连接参数创建新的数据源
         *
         * @return PostgresSaver 实例
         * @throws SQLException 如果数据库连接失败
         */
        public PostgresCheckpointStore build() throws SQLException {
            // 状态序列化器是必需的，不能为 null
            var serializer = new ObjectStreamStateSerializer<UserSessionState>( UserSessionState::new );
            // 获取序列化器的映射器 (mapper) 来注册自定义序列化器
            serializer.mapper()
                    // 为 ToolExecutionRequest 类注册自定义序列化器，以便正确处理该类型的对象
                    .register(ToolExecutionRequest.class, new ToolExecutionRequestSerializer() )
                    // 为 ChatMessage 接口注册自定义序列化器，以便正确处理所有 ChatMessage 的实现类
                    .register(ChatMessage.class, new ChatMesssageSerializer() );
            stateSerializer=serializer;
            requireNonNull(stateSerializer, "stateSerializer cannot be null");

            // 如果没有提供外部数据源，则根据参数创建新的数据源
            if (datasource == null) {
                // 端口号必须大于 0
                if( port <=0 ) {
                    throw new IllegalArgumentException("port must be greater than 0");
                }

                // 创建 PostgreSQL 简单数据源
                var ds = new PGSimpleDataSource();
                ds.setDatabaseName( requireNotBlank(database, "database"));  // 设置数据库名
                ds.setUser(requireNotBlank(user, "user"));                   // 设置用户名
                ds.setPassword(requireNonNull(password, "password cannot be null")); // 设置密码
                ds.setPortNumbers( new int[] {port} );                       // 设置端口号
                ds.setServerNames( new String[] { requireNotBlank(host, "host") } ); // 设置主机地址

                // 添加额外属性
                for (var entry : additionalProperties.entrySet()) {
                    ds.setProperty(entry.getKey().toString(), entry.getValue().toString());
                }
                datasource = ds;
            }

            // 如果设置了 dropTablesFirst，则必须创建表
            createTables = createTables || dropTablesFirst;

            // 创建 PostgresSaver 实例
            return new PostgresCheckpointStore( this );
        }
    }

    /**
     * 创建构建器实例（工厂方法入口）
     * @return 新的构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    // ============================================
    // 实例变量
    // ============================================

    /** 数据源（用于获取数据库连接） */
    protected final DataSource datasource;

    /** 状态序列化器（用于序列化/反序列化状态对象） */
    private final StateSerializer<? extends AgentState> stateSerializer;

    /** 纯文本序列化器兼容模式标志 */
    private final boolean plainTextStateSerializerLegacyMode;

    /**
     * 私有构造函数，由构建器调用
     * @param builder 构建器实例
     * @throws SQLException 如果初始化表失败
     */
    protected PostgresCheckpointStore( Builder builder ) throws SQLException {
        this.datasource = builder.datasource;                          // 设置数据源
        this.stateSerializer =  builder.stateSerializer;               // 设置序列化器
        this.plainTextStateSerializerLegacyMode = builder.plainTextStateSerializerLegacyMode; // 设置兼容模式

        initTable( builder.dropTablesFirst, builder.createTables);    // 初始化数据库表
    }

    /**
     * 回滚事务
     * 当插入或更新检查点失败时，回滚当前事务
     *
     * @param conn 数据库连接（可能为 null）
     * @param checkpoint 发生错误的检查点
     * @param threadId 线程ID
     */
    private void rollback( Connection conn, Checkpoint checkpoint, String threadId ) {
        // 如果连接为 null，无法回滚
        if (conn == null) return;

        // 检查点不能为 null
        requireNonNull(checkpoint, "checkpoint cannot be null");

        try {
            conn.rollback();  // 执行回滚
            log.warn("Transaction rolled back for checkpoint {}", checkpoint.getId());
        } catch (SQLException exRollback) {
            // 回滚失败时记录错误日志
            log.error("Failed to rollback transaction for checkpoint id {} in thread {}",
                    checkpoint.getId(),
                    threadId,
                    exRollback);
        }
    }

    /**
     * 编码状态数据为 JSON 格式
     * 将状态 Map 序列化为二进制，再转为 Base64，最后包装为 JSON 对象
     *
     * @param data 状态数据 Map
     * @return JSON 字符串，格式：{"binaryPayload": "base64编码的数据"}
     * @throws IOException 如果序列化失败
     */
    private String encodeState( Map<String,Object> data ) throws IOException {
        final byte[] binaryData;  // 存储序列化后的二进制数据

        // 根据兼容模式选择序列化方式
        if( plainTextStateSerializerLegacyMode && stateSerializer instanceof PlainTextStateSerializer<?> ser ) {
            // 兼容模式：将 JSON 转换为字符串，再转为字节数组
            binaryData = ser.writeDataAsString( data ).getBytes(StandardCharsets.UTF_8);
        }
        else {
            // 正常模式：直接序列化为二进制
            binaryData = stateSerializer.dataToBytes(data);
        }

        // 将二进制数据编码为 Base64 字符串
        final var base64Data = Base64.getEncoder().encodeToString(binaryData);

        // 返回 JSON 格式的字符串
        return """
                {"binaryPayload": "%s"}
                """.formatted(base64Data);
    }

    /**
     * 解码状态数据
     * 从数据库读取的 JSON 中提取 Base64 数据，解码后反序列化为 Map
     *
     * @param binaryPayload Base64 编码的二进制数据
     * @param contentType 内容类型（用于验证序列化器兼容性）
     * @return 状态数据 Map
     * @throws IOException 如果解码失败
     * @throws ClassNotFoundException 如果类找不到
     */
    private Map<String,Object> decodeState( byte[] binaryPayload, String contentType ) throws IOException, ClassNotFoundException {
        // 验证内容类型是否匹配
        if( !Objects.equals(contentType, stateSerializer.contentType() )) {
            throw new IllegalStateException(
                    format( "Content Type used for store state '%s' is different from one '%s' used for deserialize it",
                            contentType,
                            stateSerializer.contentType() ));
        }

        // Base64 解码
        final byte[] bytes = Base64.getDecoder().decode(binaryPayload);

        // 根据兼容模式选择反序列化方式
        if( plainTextStateSerializerLegacyMode && stateSerializer instanceof PlainTextStateSerializer<?> ser ) {
            // 兼容模式：从字符串读取
            return ser.readDataFromString( new String(bytes, StandardCharsets.UTF_8) );
        }
        // 正常模式：从字节数组读取
        return stateSerializer.dataFromBytes( bytes );
    }

    /**
     * 初始化数据库表
     * 根据配置创建或删除表结构
     *
     * @param dropTablesFirst 是否先删除表
     * @param createTables 是否创建表
     * @throws SQLException 如果 SQL 执行失败
     */
    protected void initTable(boolean dropTablesFirst, boolean createTables) throws SQLException {
        // 删除表的 SQL（级联删除依赖对象）
        var sqlDropTables = """
        DROP TABLE IF EXISTS LG4JCheckpoint CASCADE;
        DROP TABLE IF EXISTS LG4JThread CASCADE;
        """;

        // 创建表的 SQL
        var sqlCreateTables = """
                CREATE TABLE IF NOT EXISTS LG4JThread (
                     thread_id UUID PRIMARY KEY,                    -- 线程ID（UUID主键）
                     thread_name VARCHAR(255),                      -- 线程名称（用户指定的会话ID）
                     is_released BOOLEAN DEFAULT FALSE NOT NULL    -- 是否已释放（标记会话结束）
                 );
                
                 CREATE TABLE IF NOT EXISTS LG4JCheckpoint (
                     checkpoint_id UUID PRIMARY KEY,               -- 检查点ID（UUID主键）
                     parent_checkpoint_id UUID,                     -- 父检查点ID（支持分支）
                     thread_id UUID NOT NULL,                       -- 所属线程（外键）
                     node_id VARCHAR(255),                          -- 当前节点ID
                     next_node_id VARCHAR(255),                     -- 下一个节点ID
                     state_data JSONB NOT NULL,                     -- 状态数据（PostgreSQL JSONB类型，高性能）
                     state_content_type VARCHAR(100) NOT NULL,      -- 内容类型（用于验证序列化器）
                     saved_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,  -- 保存时间（带时区）
                
                     CONSTRAINT fk_thread                           -- 外键约束
                         FOREIGN KEY(thread_id)
                         REFERENCES LG4JThread(thread_id)
                         ON DELETE CASCADE                          -- 线程删除时级联删除检查点
                 );
                
                 -- 普通索引：加速按线程ID查询
                 CREATE INDEX IF NOT EXISTS idx_lg4jcheckpoint_thread_id ON LG4JCheckpoint(thread_id);
                 
                 -- 复合索引：加速按线程ID和时间排序的查询（最常用）
                 CREATE INDEX IF NOT EXISTS idx_lg4jcheckpoint_thread_id_saved_at_desc ON LG4JCheckpoint(thread_id, saved_at DESC);
                 
                 -- 唯一部分索引：确保同一线程名称只有一条未释放的记录
                 -- PostgreSQL 特有功能，只索引 is_released = FALSE 的行，减少索引大小
                 CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_lg4jthread_thread_name_unreleased  ON LG4JThread(thread_name) WHERE is_released = FALSE;
                """;


        String sqlCommand = null;  // 记录当前执行的 SQL（用于错误日志）

        // 使用 try-with-resources 自动关闭连接和语句
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {

            // 如果需要删除表，先执行删除操作
            if (dropTablesFirst) {
                log.trace( "Executing drop tables:\n---\n{}---", sqlDropTables);
                sqlCommand = sqlDropTables;
                statement.executeUpdate(sqlCommand);  // 执行删除
            }

            // 如果需要创建表，执行创建操作
            if (createTables) {
                log.trace( "Executing create tables:\n---\n{}---", sqlCreateTables);
                sqlCommand = sqlCreateTables;
                statement.executeUpdate(sqlCommand);  // 执行创建
            }
        }
        catch ( SQLException ex ) {
            // 记录错误日志并重新抛出异常
            log.error( "error executing command\n{}\n", sqlCommand, ex );
            throw ex;
        }
    }


    /**
     * 从数据库加载检查点列表
     * 根据配置中的 threadId 加载该线程的所有检查点
     *
     * @param config 运行时配置（包含线程ID）
     * @return 检查点列表（按时间倒序，最新的在前）
     * @throws Exception 如果加载失败
     */
    @Override
    protected LinkedList<Checkpoint> loadCheckpoints(RunnableConfig config) throws Exception {

        final var checkpoints = new LinkedList<Checkpoint>();  // 创建链表存储检查点

        final var threadId = threadId(config);  // 获取线程ID

        // SQL：检查未释放的线程数量
        final var sqlCheckThread = """
                SELECT COUNT(*)
                FROM LG4JThread
                WHERE thread_name = ? AND is_released = FALSE
                """;

        // SQL：查询检查点（使用 CTE 公共表表达式）
        final var sqlQueryCheckpoints = """
                WITH matched_thread AS (
                    SELECT thread_id
                    FROM LG4JThread
                    WHERE thread_name = ? AND is_released = FALSE
                )
                SELECT  c.checkpoint_id,
                        c.node_id,
                        c.next_node_id,
                        c.state_data->>'binaryPayload' AS base64_data,  -- 从 JSONB 中提取 base64 数据
                        c.state_content_type,
                        c.parent_checkpoint_id
                FROM matched_thread t
                JOIN LG4JCheckpoint c ON c.thread_id = t.thread_id
                ORDER BY c.saved_at DESC  -- 按保存时间倒序，最新的在前
                """;

        // 获取数据库连接
        try( Connection conn = getConnection() ) {

            // 第一步：检查线程是否存在且未释放
            try( PreparedStatement ps = conn.prepareStatement(sqlCheckThread) ) {
                ps.setString(1, threadId);  // 设置线程名称参数
                var resultSet = ps.executeQuery();
                resultSet.next();  // 移动到第一行
                var count = resultSet.getInt(1);  // 获取计数结果

                // 如果没有活跃线程，返回空列表
                if( count == 0 ) {
                    return checkpoints;
                }
                // 如果有多个未释放的线程（数据异常），抛出错误
                if( count > 1 ) {
                    throw new IllegalStateException( format("there are more than one Thread '%s' open (not released yet)", threadId));
                }
            }

            // 第二步：查询该线程的所有检查点
            log.trace( "Executing select checkpoints:\n---\n{}---", sqlQueryCheckpoints);
            try( PreparedStatement ps = conn.prepareStatement(sqlQueryCheckpoints) ) {
                ps.setString(1, threadId);  // 设置线程名称参数
                var rs = ps.executeQuery();

                // 遍历结果集，构建检查点对象
                while( rs.next() ) {
                    var checkpoint = Checkpoint.builder()
                            .id( rs.getString(1) )                    // checkpoint_id
                            .nodeId( rs.getString(2) )                // node_id
                            .nextNodeId( rs.getString(3) )            // next_node_id
                            .state( decodeState( rs.getBytes(4),      // 解码状态数据（Base64 → Map）
                                    rs.getString( 5) ) ) // state_content_type
                            .build();
                    checkpoints.add( checkpoint );  // 添加到链表
                }
            }

        }

        return checkpoints;  // 返回检查点列表
    }

    /**
     * 插入检查点（内部方法）
     * 在已有事务中执行插入操作
     *
     * @param conn 数据库连接（已开启事务）
     * @param config 运行时配置
     * @param checkpoints 当前检查点列表
     * @param checkpoint 要插入的检查点
     * @throws Exception 如果插入失败
     */
    private void insertCheckpoint( Connection conn, RunnableConfig config, LinkedList<Checkpoint> checkpoints, Checkpoint checkpoint) throws Exception {
        var threadId = config.threadId().orElse( THREAD_ID_DEFAULT );  // 获取线程ID，不存在则使用默认值

        // 修改：不使用 updated_at 字段的版本
        var upsertThreadSql = """
        INSERT INTO LG4JThread (thread_id, thread_name, is_released)
        VALUES (?, ?, FALSE)
        ON CONFLICT (thread_name)      
        WHERE is_released = FALSE      
        DO NOTHING
        RETURNING thread_id
        """;

        // SQL：插入检查点
        var insertCheckpointSql = """
            INSERT INTO LG4JCheckpoint(
            checkpoint_id,
            parent_checkpoint_id,
            thread_id,
            node_id,
            next_node_id,
            state_data,
            state_content_type)
            VALUES (?, ?, ?, ?, ?, ?::jsonb, ?)
            """;

        UUID threadUUID = null;

        // 第一步：尝试插入或获取现有线程
        try (PreparedStatement ps = conn.prepareStatement(upsertThreadSql)) {
            UUID newThreadId = UUID.randomUUID();
            ps.setObject(1, newThreadId, Types.OTHER);
            ps.setString(2, threadId);

            log.trace("Executing upsert thread:\n---\n{}---", upsertThreadSql);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // 插入成功，获取新创建的 thread_id
                    threadUUID = rs.getObject("thread_id", UUID.class);
                }
            }
        }

        // 第二步：如果插入失败（冲突），则查询现有的 thread_id
        if (threadUUID == null) {
            String selectThreadSql = """
            SELECT thread_id FROM LG4JThread
            WHERE thread_name = ? AND is_released = FALSE
            LIMIT 1
            """;

            try (PreparedStatement ps = conn.prepareStatement(selectThreadSql)) {
                ps.setString(1, threadId);
                log.trace("Querying existing thread:\n---\n{}---", selectThreadSql);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        threadUUID = rs.getObject("thread_id", UUID.class);
                    }
                }
            }
        }

        // 确保 threadUUID 不为 null
        if (threadUUID == null) {
            throw new SQLException("Failed to get or create thread for: " + threadId);
        }

        // 第三步：插入检查点数据
        try (PreparedStatement ps = conn.prepareStatement(insertCheckpointSql)) {
            var field = 0;
            // checkpoint_id：将字符串转换为 UUID
            ps.setObject(++field, UUID.fromString(checkpoint.getId()), Types.OTHER);
            // parent_checkpoint_id：暂时为 NULL
            ps.setNull(++field, java.sql.Types.OTHER);
            // thread_id：关联的线程 UUID
            ps.setObject(++field, threadUUID, Types.OTHER);
            // node_id：当前节点ID
            ps.setString(++field, checkpoint.getNodeId());
            // next_node_id：下一个节点ID
            ps.setString(++field, checkpoint.getNextNodeId());
            // state_data：编码后的状态数据（JSON 字符串）
            ps.setString(++field, encodeState(checkpoint.getState()));
            // state_content_type：内容类型
            ps.setString(++field, stateSerializer.contentType());

            log.trace("Executing insert checkpoint:\n---\n{}---", insertCheckpointSql);
            ps.executeUpdate();
        }
    }
    /**
     * 插入新检查点（公开方法，由父类调用）
     * 开启事务并调用内部插入方法
     *
     * @param config 运行时配置
     * @param checkpoints 当前检查点列表
     * @param checkpoint 要插入的检查点
     * @throws Exception 如果插入失败
     */
    @Override
    protected void insertedCheckpoint( RunnableConfig config, LinkedList<Checkpoint> checkpoints, Checkpoint checkpoint) throws Exception {
        var threadId = config.threadId().orElse( THREAD_ID_DEFAULT );  // 获取线程ID

        Connection conn = null;  // 数据库连接
        try( Connection ignored = conn = getConnection() )  {
            conn.setAutoCommit(false);  // 开启事务（关闭自动提交）

            insertCheckpoint( conn, config, checkpoints, checkpoint );  // 执行插入

            conn.commit();  // 提交事务
            log.debug("Checkpoint {} for thread {} inserted successfully.", checkpoint.getId(), threadId);

        } catch (SQLException | IOException e) {  // IOException 来自 encodeState
            log.error("Error inserting checkpoint with id {} in thread {}", checkpoint.getId(), threadId, e);
            rollback( conn, checkpoint, threadId );  // 回滚事务
            throw e;  // 重新抛出异常
        }

    }

    /**
     * 更新检查点（实际是删除旧的，插入新的）
     * 当检查点ID已存在时调用此方法
     *
     * @param config 运行时配置
     * @param checkpoints 当前检查点列表
     * @param checkpoint 要更新的检查点
     * @throws Exception 如果更新失败
     */
    @Override
    protected void updatedCheckpoint( RunnableConfig config,
                                      LinkedList<Checkpoint> checkpoints,
                                      Checkpoint checkpoint) throws Exception {

        final var threadId = config.threadId().orElse(THREAD_ID_DEFAULT);  // 获取线程ID

        // SQL：删除旧的检查点
        var deletePreviousCheckpointSql = """
                DELETE FROM LG4JCheckpoint
                WHERE checkpoint_id = ?;
                """;

        Connection conn = null;  // 数据库连接

        try( Connection ignored = conn = getConnection() )  {
            conn.setAutoCommit(false);  // 开启事务

            // 如果配置中指定了旧的检查点ID，则删除它
            if( config.checkPointId().isPresent() ) {

                try (PreparedStatement ps = conn.prepareStatement(deletePreviousCheckpointSql)) {
                    var field = 0;
                    ps.setObject(++field,
                            UUID.fromString(config.checkPointId().get()),  // 将字符串转换为 UUID
                            Types.OTHER);
                    log.trace( "Executing deleting previous checkpoint with id {} in thread {}:\n---\n{}---",
                            config.checkPointId().get(),
                            threadId,
                            deletePreviousCheckpointSql);
                    ps.executeUpdate();  // 执行删除
                }
            }

            insertCheckpoint( conn, config, checkpoints, checkpoint);  // 插入新的检查点

            conn.commit();  // 提交事务

            log.debug("Checkpoint with id {} for thread {} inserted successfully.",
                    checkpoint.getId(),
                    threadId);

        } catch (SQLException | IOException e) {  // IOException 来自 encodeState
            log.error("Error inserting checkpoint with id {} in thread {}",
                    checkpoint.getId(),
                    threadId,
                    e);
            rollback( conn, checkpoint, threadId );  // 回滚事务
            throw e;
        }
    }

    /**
     * 释放检查点（标记线程为已释放）
     * 会话结束后调用，释放资源
     *
     * @param config 运行时配置
     * @param checkpoints 检查点列表
     * @return 释放标签，包含线程名称和检查点列表
     * @throws Exception 如果释放失败
     */
    @Override
    protected Tag releaseCheckpoints(RunnableConfig config, LinkedList<Checkpoint> checkpoints) throws Exception {
        final var threadId = threadId( config );  // 获取线程ID

        // SQL：查询未释放的线程
        var selectThreadSql = """
               SELECT thread_id FROM LG4JThread
               WHERE thread_name = ? AND is_released = FALSE
               """;

        // SQL：释放线程（设置 is_released = TRUE）
        var releaseThreadSql = """
                UPDATE LG4JThread
                SET
                    is_released = TRUE
                WHERE thread_id = ?;
                """;

        try( Connection conn = getConnection() )  {

            UUID threadUUID = null;  // 存储线程 UUID

            // 第一步：查询未释放的线程
            try (PreparedStatement ps = conn.prepareStatement(selectThreadSql)) {
                var field = 0;
                ps.setString(++field, threadId);  // 设置线程名称

                try (ResultSet rs = ps.executeQuery()) {
                    var rows = 0;
                    while( rs.next() ) {
                        threadUUID = rs.getObject("thread_id", UUID.class);
                        ++rows;
                    }
                    // 如果没有找到活跃线程，抛出异常
                    if( rows == 0 ) {
                        throw new IllegalStateException( format("active Thread '%s' not found",threadId) );
                    }
                    // 如果找到多个活跃线程（数据异常），抛出异常
                    if( rows > 1 ) {
                        throw new IllegalStateException( format("duplicate active Thread '%s' found",threadId) );
                    }
                }
            }

            // 第二步：更新线程状态为已释放
            log.trace( "Executing release Thread:\n---\n{}---", releaseThreadSql);
            try (PreparedStatement ps = conn.prepareStatement(releaseThreadSql)) {
                var field = 0;
                ps.setObject(++field,
                        Objects.requireNonNull(threadUUID,"threadUUID cannot be null"),
                        Types.OTHER);
                ps.executeUpdate();  // 执行更新
            }
        }

        // 返回释放标签
        return new Tag( threadId, checkpoints );
    }

    /**
     * 获取数据库连接
     * 从数据源中获取新的连接
     *
     * @return 数据库连接
     * @throws SQLException 如果连接失败
     */
    protected Connection getConnection() throws SQLException {
        return datasource.getConnection();
    }

    /**
     * 清除检查点缓存（已废弃）
     * 当前实现不使用缓存，因此返回空列表
     *
     * @param threadId 线程标识
     * @return 空的检查点集合
     * @deprecated 此方法不再使用，因为当前保存器不使用缓存
     */
    @Deprecated(forRemoval = true)
    public Collection<Checkpoint> clearCheckpointsCache(String threadId ) {
        return List.of();  // 返回空列表
    }
}