package com.sky.service;


import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {
    /**
     * 分页查询套餐
     *
     * @param setmealPageQueryDTO 分页查询条件
     * @return 分页查询结果
     */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 新增套餐
     *
     * @param setmealDTO 套餐信息
     */
    void save(SetmealDTO setmealDTO);

    /**
     * 根据id查询套餐
     *
     * @param id 套餐id
     * @return 套餐信息
     */
    SetmealVO getByIdWithDish(Long id);

    /**
     * 删除套餐
     *
     * @param ids 套餐id列表
     */
    void delete(List<Long> ids);

    /**
     * 修改套餐状态
     *
     * @param status 状态
     * @param id     套餐id
     */
    void startOrStop(Integer status, Long id);

    /**
     * 修改套餐
     *
     * @param setmealDTO 套餐信息
     */
    void update(SetmealDTO setmealDTO);
}
