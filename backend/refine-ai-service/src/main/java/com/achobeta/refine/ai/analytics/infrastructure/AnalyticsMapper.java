package com.achobeta.refine.ai.analytics.infrastructure;

import com.achobeta.refine.ai.analytics.application.query.InsightRow;
import com.achobeta.refine.ai.analytics.application.query.LearningVectorRow;
import com.achobeta.refine.ai.analytics.application.query.WeaknessRow;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AnalyticsMapper {
    @Insert("INSERT IGNORE INTO consumed_events(event_id,event_type) VALUES(#{eventId},#{eventType})")
    int markConsumed(@Param("eventId") String eventId, @Param("eventType") String eventType);

    @Insert("INSERT INTO user_learning_vectors(event_id,user_id,question_id,action_type,question_content," +
            "subject,knowledge_point_id,embedding_text,embedding_model,metadata_text) VALUES(#{eventId},#{userId},#{questionId}," +
            "#{actionType},#{questionContent},#{subject},#{knowledgePointId},#{embeddingText},#{embeddingModel},#{metadataText})")
    int insertVector(@Param("eventId") String eventId, @Param("userId") String userId,
                     @Param("questionId") String questionId, @Param("actionType") String actionType,
                     @Param("questionContent") String questionContent, @Param("subject") String subject,
                     @Param("knowledgePointId") Integer knowledgePointId,
                     @Param("embeddingText") String embeddingText, @Param("embeddingModel") String embeddingModel,
                     @Param("metadataText") String metadataText);

    @Delete("DELETE FROM learning_insights WHERE user_id=#{userId}")
    int deleteInsights(String userId);

    @Insert("INSERT INTO learning_insights(user_id,insight_type,title,description,confidence_score,metadata,is_active) " +
            "VALUES(#{userId},#{type},#{title},#{description},#{confidenceScore},#{metadata},1)")
    int insertInsight(@Param("userId") String userId, @Param("type") String type,
                      @Param("title") String title, @Param("description") String description,
                      @Param("confidenceScore") double confidenceScore, @Param("metadata") String metadata);

    @Select("<script>SELECT insight_type AS type,title,description,confidence_score AS confidenceScore," +
            "metadata AS relatedQuestions,created_at AS createdAt,is_active AS isActive FROM learning_insights " +
            "WHERE user_id=#{userId} AND is_active=1" +
            "<if test='type != null'> AND insight_type=#{type}</if> ORDER BY created_at DESC,id DESC</script>")
    @ConstructorArgs({@Arg(column="type", javaType=String.class), @Arg(column="title", javaType=String.class),
            @Arg(column="description", javaType=String.class), @Arg(column="confidenceScore", javaType=double.class),
            @Arg(column="relatedQuestions", javaType=String.class), @Arg(column="createdAt", javaType=java.time.LocalDateTime.class),
            @Arg(column="isActive", javaType=boolean.class)})
    List<InsightRow> findInsights(@Param("userId") String userId, @Param("type") String type);

    @Select("SELECT question_id AS questionId,question_content AS questionContent,action_type AS actionType," +
            "subject,embedding_text AS embeddingText,embedding_model AS embeddingModel,created_at AS createdAt FROM user_learning_vectors " +
            "WHERE user_id=#{userId} ORDER BY created_at DESC,id DESC LIMIT #{limit}")
    @ConstructorArgs({@Arg(column="questionId", javaType=String.class), @Arg(column="questionContent", javaType=String.class),
            @Arg(column="actionType", javaType=String.class), @Arg(column="subject", javaType=String.class),
            @Arg(column="embeddingText", javaType=String.class), @Arg(column="embeddingModel", javaType=String.class),
            @Arg(column="createdAt", javaType=java.time.LocalDateTime.class)})
    List<LearningVectorRow> recentVectors(@Param("userId") String userId, @Param("limit") int limit);

    @Select("SELECT COALESCE(NULLIF(subject,''),'未分类') AS subject,COUNT(*) AS itemCount FROM user_learning_vectors " +
            "WHERE user_id=#{userId} AND action_type IN ('mistake','upload') GROUP BY subject ORDER BY itemCount DESC")
    @ConstructorArgs({@Arg(column="subject", javaType=String.class), @Arg(column="itemCount", javaType=long.class)})
    List<WeaknessRow> weaknessSubjects(String userId);

    @Select("SELECT COUNT(*) FROM user_learning_vectors WHERE user_id=#{userId} AND created_at >= DATE_SUB(NOW(),INTERVAL 7 DAY)")
    long recentWeekCount(String userId);

    @Select("SELECT COUNT(DISTINCT DATE(created_at)) FROM user_learning_vectors WHERE user_id=#{userId} " +
            "AND created_at >= DATE_SUB(NOW(),INTERVAL 7 DAY)")
    long recentActiveDays(String userId);

    @Select("SELECT question_id FROM user_learning_vectors WHERE user_id=#{userId} " +
            "AND COALESCE(NULLIF(subject,''),'未分类')=#{subject} ORDER BY created_at DESC LIMIT 10")
    List<String> recentQuestionIds(@Param("userId") String userId, @Param("subject") String subject);
}
