package com.sky.controller.user;

import com.sky.constant.RedisConstant;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Slf4j
public class ShopController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取店铺营业状态
     *
     * @return 店铺营业状态
     */
    @GetMapping("/status")
    public Result<Integer> getStatus() {
        Integer status = readShopStatus();
        if (status == null) {
            log.info("店铺状态不存在，设置为默认打烊中");
            status = 0;
        }
        log.info("获取店铺状态为：{}", status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }

    /**
     * 读取店铺状态
     * <p>JSON 序列化后取回来的是 Number,统一按 Number 转,避免强转类型不符
     */
    private Integer readShopStatus() {
        Object value = redisTemplate.opsForValue().get(RedisConstant.SHOP_STATUS_KEY);
        return value == null ? null : ((Number) value).intValue();
    }
}
