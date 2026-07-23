package com.achobeta.refine.learning.overview.infrastructure;

import com.achobeta.refine.learning.overview.application.query.OverviewCounts;
import com.achobeta.refine.learning.overview.application.query.StudyDynamic;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OverviewMapper {
    @Select("SELECT COUNT(*) AS questionsNum," +
            "COALESCE(100.0*SUM(CASE WHEN question_status=1 THEN 1 ELSE 0 END)/NULLIF(COUNT(*),0),0) AS reviewRate," +
            "COUNT(DISTINCT CASE WHEN knowledge_point_id IS NOT NULL THEN knowledge_point_id END) AS hardQuestions " +
            "FROM MistakeQuestion WHERE user_id=#{userId}")
    @ConstructorArgs({@Arg(column = "questionsNum", javaType = long.class),
            @Arg(column = "reviewRate", javaType = double.class), @Arg(column = "hardQuestions", javaType = long.class)})
    OverviewCounts overview(String userId);

    @Select("SELECT COALESCE(study_time,0) FROM UserData WHERE user_id=#{userId}")
    Integer studyTime(String userId);

    @Select("SELECT COUNT(*) AS uploadCount," +
            "COALESCE(SUM(CASE WHEN update_time>=DATE_SUB(NOW(),INTERVAL 7 DAY) THEN 1 ELSE 0 END),0) AS recentReviewCount " +
            "FROM MistakeQuestion WHERE user_id=#{userId}")
    @ConstructorArgs({@Arg(column = "uploadCount", javaType = long.class),
            @Arg(column = "recentReviewCount", javaType = long.class)})
    StudyDynamic dynamic(String userId);
}
