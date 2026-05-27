package com.fishseedling.platform.myLangChain4j.mongodb.mapper;


import com.fishseedling.platform.myLangChain4j.mongodb.entity.ChatMemoryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 会话记忆数据访问层
 */
@Repository
public interface ChatMemoryRepository extends MongoRepository<ChatMemoryDocument, String> {

    /**
     * 根据 memoryId 查询会话（最常用）
     */
    Optional<ChatMemoryDocument> findByMemoryId(String memoryId);

    /**
     * 根据 userType 查询所有会话
     */
    List<ChatMemoryDocument> findByUserType(String userType);

    /**
     * 删除指定 memoryId 的会话
     */
    void deleteByMemoryId(String memoryId);

    /**
     * 检查会话是否存在
     */
    boolean existsByMemoryId(String memoryId);

    /**
     * 根据 userType 统计会话数量
     */
    long countByUserType(String userType);

    /**
     * 自定义查询：查找更新时间在指定时间之前的会话（用于清理旧会话）
     */
    @Query("{ 'updated_at': { $lt: ?0 } }")
    List<ChatMemoryDocument> findByUpdatedAtBefore(Date date);

    /**
     * 分页查询管理员的会话列表（按更新时间倒序）
     */
    @Query("{ 'user_type': 'ADMIN' }")
    List<ChatMemoryDocument> findAllAdminConversations(org.springframework.data.domain.Pageable pageable);
}