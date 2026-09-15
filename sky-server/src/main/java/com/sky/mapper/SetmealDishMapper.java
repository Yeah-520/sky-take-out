package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品id查询对应的套餐id
     *
     * @param dishIds 菜品id
     * @return 套餐id
     */
    List<Long> getSetmealDishIds(List<Long> dishIds);

    /**
     * 根据套餐id查询套餐和菜品的关联关系
     *
     * @param setmealId 套餐id
     * @return 套餐和菜品的关联关系
     */
    @Select("select * from setmeal_dish where setmeal_id = #{setmealId}")
    List<SetmealDish> getBySetmealId(Long setmealId);

    @AutoFill(OperationType.INSERT)
    void insert(Setmeal setmeal);

    /**
     * 批量保存套餐和菜品的关联关系
     *
     * @param setmealDishes
     */
    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * 根据套餐id删除套餐和菜品的关联关系
     *
     * @param ids 套餐id列表
     */
    void deleteBySetmealIds(List<Long> ids);
}
