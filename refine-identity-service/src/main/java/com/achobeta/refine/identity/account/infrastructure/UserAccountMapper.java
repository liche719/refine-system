package com.achobeta.refine.identity.account.infrastructure;

import com.achobeta.refine.identity.account.domain.UserAccount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserAccountMapper {
    @Select("SELECT id,user_id,user_name,user_account,user_password,user_status,create_time " +
            "FROM UserInformation WHERE user_account=#{account}")
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "user_id", javaType = String.class),
            @Arg(column = "user_name", javaType = String.class),
            @Arg(column = "user_account", javaType = String.class),
            @Arg(column = "user_password", javaType = String.class),
            @Arg(column = "user_status", javaType = Integer.class),
            @Arg(column = "create_time", javaType = java.time.LocalDateTime.class)
    })
    UserAccount findByAccount(String account);

    @Select("SELECT id,user_id,user_name,user_account,user_password,user_status,create_time " +
            "FROM UserInformation WHERE user_id=#{userId}")
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "user_id", javaType = String.class),
            @Arg(column = "user_name", javaType = String.class),
            @Arg(column = "user_account", javaType = String.class),
            @Arg(column = "user_password", javaType = String.class),
            @Arg(column = "user_status", javaType = Integer.class),
            @Arg(column = "create_time", javaType = java.time.LocalDateTime.class)
    })
    UserAccount findById(String userId);

    @Insert("INSERT INTO UserInformation(user_id,user_name,user_account,user_password,user_status) " +
            "VALUES(#{userId},#{userName},#{userAccount},#{passwordHash},#{status})")
    int insert(UserAccount account);

    @Update("UPDATE UserInformation SET user_password=#{passwordHash} WHERE user_id=#{userId}")
    int updatePasswordById(@Param("userId") String userId, @Param("passwordHash") String passwordHash);

    @Update("UPDATE UserInformation SET user_password=#{passwordHash} WHERE user_account=#{account}")
    int updatePasswordByAccount(@Param("account") String account, @Param("passwordHash") String passwordHash);
}
