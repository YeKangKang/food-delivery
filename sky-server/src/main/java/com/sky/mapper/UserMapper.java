package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    /**
     * 通过openid查询用户
     * @param openId
     * @return
     */
    @Select("select * from user where openid = #{openId}")
    User getByOpenId(@Param("openId") String openId);


    /**
     * 向数据库插入新的user（注册）
     * @param user
     */
    void insert(User user);

    @Select("select * from user where id = #{userId}")
    User getById(@Param("userId") Long userId);
}
