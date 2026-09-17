package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {

    /**
     * 条件查询菜品和口味
     *
     * @param dish 菜品
     * @return 菜品和口味信息
     */
    List<DishVO> listWithFlavor(Dish dish);


    /**
     * 新增菜品，同时保存对应的口味数据
     */
    void saveWithFlavor(DishDTO dishDTO);

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO 菜品分页查询条件
     * @return 菜品分页查询结果
     */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 批量删除菜品
     *
     * @param ids 菜品id列表
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根据id查询菜品详情
     *
     * @param id 菜品id
     * @return 菜品详情
     */
    DishVO getByIdWithFlavor(Long id);

    /**
     * 根据id修改菜品
     *
     * @param dishDTO 菜品信息
     */
    void updateWithFlavor(DishDTO dishDTO);

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId 分类id
     * @return 菜品列表
     */
    List<Dish> list(Long categoryId);

    /**
     * 根据id修改菜品状态
     *
     * @param status 菜品状态
     * @param id     菜品id
     */
    void updateStatus(Integer status, Long id);
}
