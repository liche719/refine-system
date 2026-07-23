package com.achobeta.refine.learning.knowledge.infrastructure;

import com.achobeta.refine.contracts.learning.RecentKnowledgePoint;
import com.achobeta.refine.learning.knowledge.application.query.KnowledgeSummary;
import com.achobeta.refine.learning.knowledge.application.query.KnowledgeTooltip;
import com.achobeta.refine.learning.knowledge.application.query.RelatedQuestion;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface KnowledgeMapper {
    @Select("<script>SELECT knowledge_point_id AS id,knowledge_point_name AS keyPoints FROM knowledgePoint " +
            "WHERE user_id=#{userId} AND parent_knowledge_point_id IS NULL" +
            "<if test='subject != null and subject != &quot;&quot;'> AND subject=#{subject}</if> ORDER BY update_time DESC</script>")
    @ConstructorArgs({@Arg(column = "id", javaType = int.class), @Arg(column = "keyPoints", javaType = String.class)})
    List<KnowledgeSummary> roots(@Param("userId") String userId, @Param("subject") String subject);

    @Select("SELECT knowledge_point_id AS id,knowledge_point_name AS keyPoints FROM knowledgePoint " +
            "WHERE user_id=#{userId} AND parent_knowledge_point_id=#{parentId} ORDER BY knowledge_point_id")
    @ConstructorArgs({@Arg(column = "id", javaType = int.class), @Arg(column = "keyPoints", javaType = String.class)})
    List<KnowledgeSummary> children(@Param("userId") String userId, @Param("parentId") int parentId);

    @Select("SELECT knowledge_desc FROM knowledgePoint WHERE user_id=#{userId} AND knowledge_point_id=#{id}")
    String description(@Param("userId") String userId, @Param("id") int id);

    @Select("SELECT knowledge_point_id AS id,knowledge_point_name AS name,knowledge_desc AS description " +
            "FROM knowledgePoint WHERE user_id=#{userId} ORDER BY update_time DESC LIMIT #{limit}")
    List<RecentKnowledgePoint> recent(@Param("userId") String userId, @Param("limit") int limit);

    @Select("SELECT id,question_content AS question FROM MistakeQuestion " +
            "WHERE user_id=#{userId} AND knowledge_point_id=#{knowledgeId} ORDER BY update_time DESC")
    @ConstructorArgs({@Arg(column = "id", javaType = long.class), @Arg(column = "question", javaType = String.class)})
    List<RelatedQuestion> relatedQuestions(@Param("userId") String userId, @Param("knowledgeId") int knowledgeId);

    @Select("SELECT note FROM knowledgePoint WHERE user_id=#{userId} AND knowledge_point_id=#{knowledgeId}")
    String note(@Param("userId") String userId, @Param("knowledgeId") int knowledgeId);

    @Select("SELECT COUNT(*) AS total,COALESCE(SUM(CASE WHEN question_status=1 THEN 1 ELSE 0 END),0) AS count," +
            "MAX(update_time) AS lastReviewTime FROM MistakeQuestion WHERE user_id=#{userId} AND knowledge_point_id=#{knowledgeId}")
    @ConstructorArgs({@Arg(column = "total", javaType = long.class), @Arg(column = "count", javaType = long.class),
            @Arg(column = "lastReviewTime", javaType = java.time.LocalDateTime.class)})
    KnowledgeTooltip tooltip(@Param("userId") String userId, @Param("knowledgeId") int knowledgeId);

    @Update("UPDATE knowledgePoint SET status=1 WHERE user_id=#{userId} AND knowledge_point_id=#{knowledgeId}")
    int markMastered(@Param("userId") String userId, @Param("knowledgeId") int knowledgeId);

    @Update("UPDATE knowledgePoint SET note=#{note} WHERE user_id=#{userId} AND knowledge_point_id=#{knowledgeId}")
    int updateNote(@Param("userId") String userId, @Param("knowledgeId") int knowledgeId, @Param("note") String note);

    @Update("UPDATE knowledgePoint SET knowledge_point_name=#{name} WHERE user_id=#{userId} AND knowledge_point_id=#{knowledgeId}")
    int rename(@Param("userId") String userId, @Param("knowledgeId") int knowledgeId, @Param("name") String name);

    @Insert("INSERT INTO knowledgePoint(user_id,knowledge_point_id,parent_knowledge_point_id,knowledge_point_name,knowledge_desc,subject,knowledge_level) " +
            "VALUES(#{userId},#{id},#{parentId},#{name},#{description},#{subject},2)")
    int addChild(@Param("userId") String userId, @Param("id") int id, @Param("parentId") int parentId,
                 @Param("name") String name, @Param("description") String description, @Param("subject") String subject);

    @Select("SELECT knowledge_point_id FROM knowledgePoint WHERE user_id=#{userId} AND subject=#{subject} " +
            "AND knowledge_point_name=#{name} AND parent_knowledge_point_id IS NULL LIMIT 1")
    Integer findRootId(@Param("userId") String userId, @Param("subject") String subject, @Param("name") String name);

    @Insert("INSERT INTO knowledgePoint(user_id,knowledge_point_id,parent_knowledge_point_id,knowledge_point_name,knowledge_desc,subject,knowledge_level) " +
            "VALUES(#{userId},#{id},NULL,#{name},#{description},#{subject},1)")
    int addRoot(@Param("userId") String userId, @Param("id") int id, @Param("name") String name,
                @Param("description") String description, @Param("subject") String subject);

    @Select("SELECT COALESCE(MAX(knowledge_point_id),0)+1 FROM knowledgePoint WHERE user_id=#{userId}")
    int nextId(@Param("userId") String userId);
}
