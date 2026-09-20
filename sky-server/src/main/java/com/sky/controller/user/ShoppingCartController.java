package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/user/shoppingCart")
@RestController
@Slf4j
public class ShoppingCartController {


    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 添加购物车
     *
     * @param shoppingCartDTO 购物车数据
     * @return 购物车数量
     */
    @PostMapping("/add")
    public Result<String> add(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("添加购物车：{}", shoppingCartDTO);
        shoppingCartService.addShoppingCart(shoppingCartDTO);
        return Result.success();
    }

    /**
     * 购物车列表
     *
     * @return 购物车列表
     */
    @GetMapping("/list")
    public Result<List<ShoppingCart>> list() {
        log.info("查询购物车列表");
        return Result.success(shoppingCartService.list());
    }

    /**
     * 清空购物车
     *
     * @return 清空购物车结果
     */
    @DeleteMapping("/clean")
    public Result<String> clean() {
        log.info("清空购物车");
        shoppingCartService.cleanShoppingCart();
        return Result.success();
    }

    /**
     * 删除购物车
     * @param shoppingCartDTO 购物车数据
     * @return 删除结果
     */
    @PostMapping("/sub")
    public Result<String> delete(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("删除购物车：{}", shoppingCartDTO);
        shoppingCartService.deleteShoppingCart(shoppingCartDTO);
        return Result.success();
    }
}
