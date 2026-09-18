package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/admin/dish")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     *
     * @param dishDTO 菜品信息
     * @return 新增菜品的id
     */
    @PostMapping
    public Result<String> save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);

        // 清理缓存
        String key = "dish_" + dishDTO.getCategoryId();
        cleanCache(key);
        return Result.success();
    }

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO 菜品分页查询条件
     * @return 菜品分页查询结果
     */
    @GetMapping("/page")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("分页查询菜品，第{}页，每页{}条，名称：{}", dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize(), dishPageQueryDTO.getName());
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 菜品批量删除
     *
     * @param ids 菜品id列表
     * @return 删除结果
     */
    @DeleteMapping
    public Result<String> delete(@RequestParam List<Long> ids) {
        log.info("删除菜品：{}", ids);
        dishService.deleteBatch(ids);

        // 清理缓存，以dish_为前缀的key
        cleanCache("dish_*");
        return Result.success();
    }

    /**
     * 根据id查询菜品详情
     *
     * @param id 菜品id
     * @return 菜品详情
     */
    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable Long id) {
        log.info("根据id查询菜品详情：{}", id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 修改菜品
     *
     * @param dishDTO 菜品信息
     * @return 修改结果
     */
    @PutMapping
    public Result<String> update(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品：{}", dishDTO);
        dishService.updateWithFlavor(dishDTO);

        // 清理缓存，以dish_为前缀的key
        cleanCache("dish_*");

        return Result.success();
    }

    /**
     * 根据分类id查询菜品列表
     *
     * @param categoryId 分类id
     * @return 菜品列表
     */
    @GetMapping("/list")
    public Result<List<Dish>> list(Long categoryId) {
        log.info("根据分类id查询菜品列表：{}", categoryId);
        return Result.success(dishService.list(categoryId));
    }

    /**
     * 根据id修改菜品状态
     *
     * @param status 状态
     * @param id     菜品id
     * @return 修改结果
     */
    @PostMapping("/status/{status}")
    public Result<String> updateStatus(@PathVariable Integer status, Long id) {
        log.info("修改菜品状态，status:{}, id:{}", status, id);
        dishService.updateStatus(status, id);

        // 清理缓存，以dish_为前缀的key
        cleanCache("dish_*");

        return Result.success();
    }


    /**
     * 清理缓存
     *
     * @param pattern 缓存前缀
     */
    private void cleanCache(String pattern) {
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}
