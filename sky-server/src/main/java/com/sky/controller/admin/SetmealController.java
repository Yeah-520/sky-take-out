package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("admin/setmeal")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    /**
     * 分页查询套餐
     *
     * @param setmealPageQueryDTO 分页查询条件
     * @return 分页查询结果
     */
    @GetMapping("/page")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("分页查询套餐，参数：{}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 新增套餐
     *
     * @param setmealDTO 套餐信息
     * @return 新增结果
     */
    @PostMapping
    @CacheEvict(cacheNames = "setmealCache", key = "#setmealDTO.categoryId")
    public Result<String> save(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐:{}", setmealDTO);
        setmealService.save(setmealDTO);
        return Result.success();
    }

    /**
     * 根据id查询套餐
     *
     * @param id 套餐id
     * @return 套餐信息
     */
    @GetMapping("/{id}")
    public Result<SetmealVO> get(@PathVariable Long id) {
        log.info("根据id查询套餐，id:{}", id);
        return Result.success(setmealService.getByIdWithDish(id));
    }

    /**
     * 删除套餐
     *
     * @param ids 套餐id列表
     * @return 删除结果
     */
    @DeleteMapping
    @CacheEvict(cacheNames = "setmealCache",allEntries = true)
    public Result<String> delete(@RequestParam List<Long> ids) {
        log.info("删除套餐，ids:{}", ids);
        setmealService.delete(ids);
        return Result.success();
    }

    /**
     * 修改套餐状态
     *
     * @param status 状态
     * @param id     套餐id
     * @return 修改结果
     */
    @PostMapping("status/{status}")
    @CacheEvict(cacheNames = "setmealCache",allEntries = true)
    public Result<String> status(@PathVariable Integer status, Long id) {
        log.info("修改套餐状态，status:{}, id:{}", status, id);
        setmealService.startOrStop(status, id);
        return Result.success();
    }

    /**
     * 修改套餐
     *
     * @param setmealDTO 套餐信息
     * @return 修改结果
     */
    @PutMapping
    @CacheEvict(cacheNames = "setmealCache",allEntries = true)
    public Result<String> update(@RequestBody SetmealDTO setmealDTO) {
        log.info("修改套餐:{}", setmealDTO);
        setmealService.update(setmealDTO);
        return Result.success();
    }
}
