package com.sky.controller.admin;

import com.sky.constant.RedisConstant;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Slf4j
public class ShopController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置店铺状态
     *
     * @param status 店铺状态
     * @return Result<String>
     */
    @PutMapping("/{status}")
    public Result<String> setStatue(@PathVariable Integer status) {
        log.info("设置店铺状态为：{}", status == 1 ? "营业中" : "打烊中");
        // 店铺状态是业务状态而非缓存:这里**不加过期时间**,否则过期后会变成"打烊"
        redisTemplate.opsForValue().set(RedisConstant.SHOP_STATUS_KEY, status);
        return Result.success();
    }

    /**
     * 获取店铺状态
     *
     * @return Result<Integer>
     */
    @GetMapping("/status")
    public Result<Integer> getStatus() {
        Integer status = readShopStatus();
        if (status == null) {
            return Result.error("店铺状态不存在");
        }
        log.info("获取店铺状态为：{}", status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }

    /**
     * 读取店铺状态
     * <p>JSON 序列化后取回来的是 Number,统一按 Number 转,避免再次强转类型不符
     */
    private Integer readShopStatus() {
        Object value = redisTemplate.opsForValue().get(RedisConstant.SHOP_STATUS_KEY);
        return value == null ? null : ((Number) value).intValue();
    }
}
