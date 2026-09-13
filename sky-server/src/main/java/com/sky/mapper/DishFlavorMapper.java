package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DishFlavorMapper {
    /**
     * 批量插入菜品口味数据
     *
     * @param flavors 菜品口味数据
     */
    void insertBatch(List<DishFlavor> flavors);

    /**
     * 根据菜品id删除口味数据
     *
     * @param dishId 菜品id
     */
    @Delete("delete from dish_flavor where dish_id = #{id}")
    void deleteByDishId(Long dishId);

    /**
     * 根据菜品id批量删除口味数据
     * @param dishIds 菜品id列表
     */
    void deleteByDishIds(List<Long> dishIds);
}
