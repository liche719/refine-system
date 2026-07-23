package com.achobeta.refine.learning.mistake.infrastructure;

import com.achobeta.refine.contracts.learning.GenerationContextResponse;
import com.achobeta.refine.learning.mistake.application.query.NamedCount;
import com.achobeta.refine.learning.mistake.application.query.ReviewTrend;
import com.achobeta.refine.learning.mistake.application.query.TrickyKnowledge;
import com.achobeta.refine.learning.mistake.domain.MistakeQuestion;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MistakeMapper {
    @Insert("INSERT INTO MistakeQuestion(user_id,question_id,question_content,subject,knowledge_point_id,source) " +
            "VALUES(#{userId},#{questionId},#{questionContent},#{subject},#{knowledgePointId},#{source})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MutableMistake mistake);

    @Select("SELECT * FROM MistakeQuestion WHERE user_id=#{userId} AND question_id=#{questionId}")
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "user_id", javaType = String.class),
            @Arg(column = "question_id", javaType = String.class),
            @Arg(column = "question_content", javaType = String.class),
            @Arg(column = "subject", javaType = String.class),
            @Arg(column = "is_careless", javaType = Integer.class),
            @Arg(column = "is_unfamiliar", javaType = Integer.class),
            @Arg(column = "is_calculate_err", javaType = Integer.class),
            @Arg(column = "is_time_shortage", javaType = Integer.class),
            @Arg(column = "other_reason_flag", javaType = Integer.class),
            @Arg(column = "other_reason", javaType = String.class),
            @Arg(column = "knowledge_point_id", javaType = Integer.class),
            @Arg(column = "study_note", javaType = String.class),
            @Arg(column = "question_status", javaType = Integer.class),
            @Arg(column = "source", javaType = String.class),
            @Arg(column = "create_time", javaType = java.time.LocalDateTime.class),
            @Arg(column = "update_time", javaType = java.time.LocalDateTime.class)
    })
    MistakeQuestion findByUserAndQuestion(@Param("userId") String userId, @Param("questionId") String questionId);

    @Select("SELECT m.id AS mistakeQuestionId,m.subject,m.knowledge_point_id AS knowledgePointId," +
            "COALESCE(k.knowledge_point_name,'未分类知识点') AS knowledgePointName,m.question_content AS questionContent " +
            "FROM MistakeQuestion m LEFT JOIN knowledgePoint k ON k.user_id=m.user_id AND k.knowledge_point_id=m.knowledge_point_id " +
            "WHERE m.id=#{id} AND m.user_id=#{userId}")
    GenerationContextResponse findGenerationContext(@Param("id") long id, @Param("userId") String userId);

    @Select("<script>SELECT m.* FROM MistakeQuestion m LEFT JOIN knowledgePoint k " +
            "ON k.user_id=m.user_id AND k.knowledge_point_id=m.knowledge_point_id WHERE m.user_id=#{userId}" +
            "<if test='keyword != null and keyword != &quot;&quot;'> AND (m.question_content LIKE CONCAT('%',#{keyword},'%') " +
            "OR m.other_reason LIKE CONCAT('%',#{keyword},'%') OR k.knowledge_desc LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "<if test='subjects != null and !subjects.isEmpty()'> AND m.subject IN " +
            "<foreach collection='subjects' item='item' open='(' separator=',' close=')'>#{item}</foreach></if>" +
            "<if test='careless || unfamiliar || calculationError || timeShortage || other'> AND (" +
            "<if test='careless'>m.is_careless=1</if><if test='unfamiliar'><if test='careless'> OR </if>m.is_unfamiliar=1</if>" +
            "<if test='calculationError'><if test='careless || unfamiliar'> OR </if>m.is_calculate_err=1</if>" +
            "<if test='timeShortage'><if test='careless || unfamiliar || calculationError'> OR </if>m.is_time_shortage=1</if>" +
            "<if test='other'><if test='careless || unfamiliar || calculationError || timeShortage'> OR </if>m.other_reason_flag=1</if>)</if>" +
            "<if test='startDate != null'> AND m.update_time &gt;= #{startDate}</if>" +
            "<if test='endDate != null'> AND m.update_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>" +
            " AND m.question_status=0 ORDER BY m.update_time DESC LIMIT #{size} OFFSET #{offset}</script>")
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "user_id", javaType = String.class),
            @Arg(column = "question_id", javaType = String.class),
            @Arg(column = "question_content", javaType = String.class),
            @Arg(column = "subject", javaType = String.class),
            @Arg(column = "is_careless", javaType = Integer.class),
            @Arg(column = "is_unfamiliar", javaType = Integer.class),
            @Arg(column = "is_calculate_err", javaType = Integer.class),
            @Arg(column = "is_time_shortage", javaType = Integer.class),
            @Arg(column = "other_reason_flag", javaType = Integer.class),
            @Arg(column = "other_reason", javaType = String.class),
            @Arg(column = "knowledge_point_id", javaType = Integer.class),
            @Arg(column = "study_note", javaType = String.class),
            @Arg(column = "question_status", javaType = Integer.class),
            @Arg(column = "source", javaType = String.class),
            @Arg(column = "create_time", javaType = java.time.LocalDateTime.class),
            @Arg(column = "update_time", javaType = java.time.LocalDateTime.class)
    })
    List<MistakeQuestion> findPage(@Param("userId") String userId, @Param("keyword") String keyword,
                                   @Param("subjects") List<String> subjects,
                                   @Param("careless") boolean careless, @Param("unfamiliar") boolean unfamiliar,
                                   @Param("calculationError") boolean calculationError,
                                   @Param("timeShortage") boolean timeShortage, @Param("other") boolean other,
                                   @Param("startDate") java.time.LocalDate startDate,
                                   @Param("endDate") java.time.LocalDate endDate,
                                   @Param("offset") int offset, @Param("size") int size);

    @Select("<script>SELECT COUNT(*) FROM MistakeQuestion m LEFT JOIN knowledgePoint k " +
            "ON k.user_id=m.user_id AND k.knowledge_point_id=m.knowledge_point_id WHERE m.user_id=#{userId}" +
            "<if test='keyword != null and keyword != &quot;&quot;'> AND (m.question_content LIKE CONCAT('%',#{keyword},'%') " +
            "OR m.other_reason LIKE CONCAT('%',#{keyword},'%') OR k.knowledge_desc LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "<if test='subjects != null and !subjects.isEmpty()'> AND m.subject IN " +
            "<foreach collection='subjects' item='item' open='(' separator=',' close=')'>#{item}</foreach></if>" +
            "<if test='careless || unfamiliar || calculationError || timeShortage || other'> AND (" +
            "<if test='careless'>m.is_careless=1</if><if test='unfamiliar'><if test='careless'> OR </if>m.is_unfamiliar=1</if>" +
            "<if test='calculationError'><if test='careless || unfamiliar'> OR </if>m.is_calculate_err=1</if>" +
            "<if test='timeShortage'><if test='careless || unfamiliar || calculationError'> OR </if>m.is_time_shortage=1</if>" +
            "<if test='other'><if test='careless || unfamiliar || calculationError || timeShortage'> OR </if>m.other_reason_flag=1</if>)</if>" +
            "<if test='startDate != null'> AND m.update_time &gt;= #{startDate}</if>" +
            "<if test='endDate != null'> AND m.update_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>" +
            " AND m.question_status=0</script>")
    long count(@Param("userId") String userId, @Param("keyword") String keyword,
               @Param("subjects") List<String> subjects,
               @Param("careless") boolean careless, @Param("unfamiliar") boolean unfamiliar,
               @Param("calculationError") boolean calculationError,
               @Param("timeShortage") boolean timeShortage, @Param("other") boolean other,
               @Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);

    @Select("SELECT COUNT(*) FROM MistakeQuestion WHERE user_id=#{userId} AND update_time < DATE_SUB(NOW(), INTERVAL 7 DAY)")
    long overdueCount(String userId);

    @Select("SELECT m.knowledge_point_id AS knowledgeId,COALESCE(k.knowledge_point_name,'未分类') AS knowledgeName " +
            "FROM MistakeQuestion m LEFT JOIN knowledgePoint k ON k.user_id=m.user_id AND k.knowledge_point_id=m.knowledge_point_id " +
            "WHERE m.user_id=#{userId} AND m.create_time >= DATE_SUB(NOW(),INTERVAL 30 DAY) " +
            "GROUP BY m.knowledge_point_id,k.knowledge_point_name HAVING COUNT(*)>=3 ORDER BY COUNT(*) DESC LIMIT 10")
    @ConstructorArgs({@Arg(column = "knowledgeId", javaType = Integer.class), @Arg(column = "knowledgeName", javaType = String.class)})
    List<TrickyKnowledge> trickyKnowledge(String userId);

    @Select("SELECT subject AS name,COUNT(*) AS count FROM MistakeQuestion WHERE user_id=#{userId} GROUP BY subject")
    @ConstructorArgs({@Arg(column = "name", javaType = String.class), @Arg(column = "count", javaType = long.class)})
    List<NamedCount> subjectStats(String userId);

    @Select("SELECT COALESCE(k.knowledge_point_name,'') AS name,COUNT(*) AS count FROM MistakeQuestion m " +
            "LEFT JOIN knowledgePoint k ON k.user_id=m.user_id AND k.knowledge_point_id=m.knowledge_point_id " +
            "WHERE m.user_id=#{userId} AND m.question_status=0 GROUP BY m.knowledge_point_id,k.knowledge_point_name")
    @ConstructorArgs({@Arg(column = "name", javaType = String.class), @Arg(column = "count", javaType = long.class)})
    List<NamedCount> knowledgeStats(String userId);

    @Select("SELECT DATE_FORMAT(create_time,'%Y-%m') AS month,COUNT(*) AS total," +
            "SUM(CASE WHEN question_status=1 THEN 1 ELSE 0 END) AS reviewed " +
            "FROM MistakeQuestion WHERE user_id=#{userId} GROUP BY DATE_FORMAT(create_time,'%Y-%m') ORDER BY month")
    @ConstructorArgs({@Arg(column = "month", javaType = String.class), @Arg(column = "total", javaType = long.class),
            @Arg(column = "reviewed", javaType = long.class)})
    List<ReviewTrend> reviewTrend(String userId);

    @Update("UPDATE MistakeQuestion SET is_careless=#{isCareless},is_unfamiliar=#{isUnfamiliar}," +
            "is_calculate_err=#{isCalculateErr},is_time_shortage=#{isTimeShortage}," +
            "other_reason_flag=#{otherReasonFlag},other_reason=#{otherReason} WHERE user_id=#{userId} AND question_id=#{questionId}")
    int updateReasons(MistakeQuestion mistake);

    @Update("UPDATE MistakeQuestion SET study_note=#{note} WHERE user_id=#{userId} AND question_id=#{questionId}")
    int updateNote(@Param("userId") String userId, @Param("questionId") String questionId, @Param("note") String note);

    @Delete("<script>DELETE FROM MistakeQuestion WHERE user_id=#{userId} AND id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int deleteBatch(@Param("userId") String userId, @Param("ids") List<Long> ids);

    final class MutableMistake {
        public Long id;
        public String userId;
        public String questionId;
        public String questionContent;
        public String subject;
        public Integer knowledgePointId;
        public String source;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }
}
