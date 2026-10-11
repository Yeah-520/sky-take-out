package com.sky.controller.user;

import com.sky.constant.RedisConstant;
import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId 分类id
     * @return 菜品列表
     */
    @GetMapping("/list")
    public Result<List<DishVO>> list(Long categoryId) {

        String key = RedisConstant.DISH_CACHE_KEY_PREFIX + categoryId;

        // 命中缓存直接返回(包含"空列表"这种结果,用于防缓存穿透)
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.info("从缓存中获取菜品列表");
            return Result.success(toDishVOList(cached));
        }

        log.info("从数据库中获取菜品列表");
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品

        List<DishVO> list = dishService.listWithFlavor(dish);

        if (list.isEmpty()) {
            // 空结果也缓存(短 TTL):不存在的分类不再每次都打到数据库,防止缓存穿透
            log.info("分类 {} 下没有起售菜品,缓存空结果 {} 秒", categoryId, RedisConstant.EMPTY_CACHE_TTL_SECONDS);
            redisTemplate.opsForValue().set(key, list, RedisConstant.EMPTY_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } else {
            // 加 TTL,避免菜品改过之后缓存长期不失效(admin 侧 cleanCache 是第一道失效手段)
            log.info("将菜品列表缓存到Redis");
            redisTemplate.opsForValue().set(key, list, RedisConstant.DISH_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        }

        return Result.success(list);
    }

    /**
     * 把缓存里的值还原成菜品列表
     * <p>value 用 JSON 序列化且**带类型信息**(见 RedisConfiguration#jsonSerializer),
     * 所以这里能还原成真正的 {@code List<DishVO>};若换成不带类型信息的序列化器,
     * 取出来会是 {@code List<LinkedHashMap>},强转就会失败。
     */
    @SuppressWarnings("unchecked")
    private List<DishVO> toDishVOList(Object cached) {
        return (List<DishVO>) cached;
    }

}
