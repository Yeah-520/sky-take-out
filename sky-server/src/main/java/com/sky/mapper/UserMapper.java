package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户
     *
     * @param openid 微信openid
     * @return 用户对象
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 新增用户
     * @param user 用户对象
     */
    void insert(User user);

    /**
     * 根据id查询用户
     * @param userId 用户id
     * @return 用户对象
     */
    @Select("select * from user where id = #{userId}")
    User getById(Long userId);

    /**
     * 根据条件查询用户数量
     * @param map 查询条件
     * @return 用户数量
     */
    Integer countByMap(Map<String, Object> map);

}
