package com.urine.cell_seg_sys.mapper;

import com.urine.cell_seg_sys.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper // 告诉 Spring Boot 这是一个 MyBatis 的 Mapper
public interface UserMapper {

    /**
     * 根据用户名查询用户（用于登录）
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    User findByUsername(String username);

    /**
     * 新增用户（用于注册）
     * @Options: 插入后自动把生成的 userId 回填到 User 对象中
     */
    @Insert("INSERT INTO sys_user(username, password, nickname) VALUES(#{username}, #{password}, #{nickname})")
    @Options(useGeneratedKeys = true, keyProperty = "userId")
    int insert(User user);
}