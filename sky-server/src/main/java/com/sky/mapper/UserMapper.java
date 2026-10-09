package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
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
     *
     * @param user 用户对象
     */
    void insert(User user);

    /**
     * 根据id查询用户
     *
     * @param userId 用户id
     * @return 用户对象
     */
    @Select("select * from user where id = #{userId}")
    User getById(Long userId);

    /**
     * 根据条件查询用户数量
     *
     * @param map 查询条件
     * @return 用户数量
     */
    Integer countByMap(Map<String, Object> map);

    /**
     * 按"天"分组统计新增用户:一次查询替代"循环 N 天逐天查库"
     *
     * @param begin 注册时间起(含)
     * @param end   注册时间止(含)
     * @return 每行两个字段:{@code d}(yyyy-MM-dd 字符串)、{@code cnt}(当日新增用户数)
     */
    List<Map<String, Object>> countGroupByDate(@Param("begin") LocalDateTime begin,
                                               @Param("end") LocalDateTime end);

}
