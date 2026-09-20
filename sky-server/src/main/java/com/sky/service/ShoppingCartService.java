package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {

    /**
     * 添加购物车
     * @param shoppingCartDTO 购物车数据传输对象
     */
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    /**
     * 购物车列表
     * @return 购物车列表
     */
    List<ShoppingCart> list();

    /**
     * 清空购物车
     */
    void cleanShoppingCart();

}
