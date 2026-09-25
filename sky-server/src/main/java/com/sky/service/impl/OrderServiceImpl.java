package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {


    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    /**
     * 提交订单
     *
     * @param ordersSubmitDTO 订单提交参数
     * @return 订单提交结果
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        // 处理业务异常
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            log.error("错误：地址簿为空");
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId = BaseContext.getCurrentId();

        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null) {
            log.error("错误：购物车为空");
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }


        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(addressBook.getDetail());
        orders.setUserId(userId);

        orderMapper.insert(orders);


        List<OrderDetail> orderDetailList = new ArrayList<>();
        // 向订单明细表插入数据
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetailList);

        // 清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderAmount(orders.getAmount())
                .orderNumber(orders.getNumber())
                .build();
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO 订单支付参数
     * @return 订单支付结果
     */
    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {

        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        // 模拟数据
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = RandomStringUtils.randomNumeric(32);

        // 这两个很复杂没法模拟，在前端支付部分跳过验证了，这里写全了为了不爆红
        String prepayId = "1111111111";
        String packageSign = "fakeData";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("timeStamp", timeStamp);
        jsonObject.put("nonceStr", nonceStr);
        jsonObject.put("package", "prepay_id=" + prepayId);
        jsonObject.put("signType", "RSA");
        jsonObject.put("paySign", packageSign);
        jsonObject.put("code", "FAKEPAID");

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        paySuccess(ordersPaymentDTO.getOrderNumber(), true);
        return vo;
    }


    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo 订单号
     */
    @Override
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }


    /**
     * 虚假的支付成功，手动修改数据库内容：order表的付款字段相关内容
     *
     * @param outTradeNo 订单号
     */
    @Override
    public void paySuccess(String outTradeNo, boolean isFake) {

        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单号查询当前用户的订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.fakeUpdate(orders);
    }

    /**
     * 根据条件查询订单
     *
     * @param ordersPageQueryDTO 订单查询条件
     * @return 订单分页查询结果
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(
                ordersPageQueryDTO.getPage(),
                ordersPageQueryDTO.getPageSize()
        );

        Page<Orders> page = orderMapper.conditionSearch(ordersPageQueryDTO);

        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 取消订单
     *
     * @param ordersCancelDTO 订单取消参数
     */
    @Override
    public void cancelOrder(OrdersCancelDTO ordersCancelDTO) {
        Orders orders = orderMapper.getById(ordersCancelDTO.getId());
        // 待付款、待派送、派送中、已完成状态可以进行取消操作，进行取消操作需要选择取消原因
        // TODO 还需补充退款操作
        if (orders.getStatus() == Orders.PENDING_PAYMENT || orders.getStatus() == Orders.TO_BE_CONFIRMED || orders.getStatus() == Orders.DELIVERY_IN_PROGRESS || orders.getStatus() == Orders.COMPLETED) {
            orders.setId(ordersCancelDTO.getId());
            orders.setCancelReason(ordersCancelDTO.getCancelReason());
            orders.setCancelTime(LocalDateTime.now());
            orders.setStatus(Orders.CANCELLED);
            orderMapper.cancelOrder(orders);
            log.info("等待退款，订单号：{}", orders.getNumber());
        } else {
            log.error("错误：订单状态不允许取消");
        }
    }

    /**
     * 根据id查询订单
     *
     * @param id 订单id
     * @return 订单详情
     */
    @Override
    public OrderVO details(Long id) {
        Orders orders = orderMapper.getById(id);

        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 获取订单统计信息
     *
     * @return 订单统计信息
     */
    @Override
    public OrderStatisticsVO getStatistics() {
        // 待派送数量 派送中数量 待接单数量
        Integer toBeConfirmed = orderMapper.getStatistics(Orders.TO_BE_CONFIRMED);
        Integer deliveryInProgress = orderMapper.getStatistics(Orders.DELIVERY_IN_PROGRESS);
        Integer confirmed = orderMapper.getStatistics(Orders.CONFIRMED);

        return OrderStatisticsVO.builder()
                .toBeConfirmed(toBeConfirmed)
                .confirmed(confirmed)
                .deliveryInProgress(deliveryInProgress)
                .build();
    }

    /**
     * 确认订单
     *
     * @param id 订单id
     */
    @Override
    public void confirmOrder(Long id) {
        Orders orders = new Orders();
        orders.setId(id);
        orders.setStatus(Orders.CONFIRMED);
        orderMapper.update(orders);
    }

    /**
     * 配送订单
     *
     * @param id 订单id
     */
    @Override
    public void delivery(Long id) {
        Orders orders = new Orders();
        orders.setId(id);
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);
    }

    /**
     * 拒绝订单
     *
     * @param ordersRejectionDTO 拒绝订单参数
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders orders = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .cancelReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 完成订单
     *
     * @param id 订单id
     */
    @Override
    public void completeOrder(Long id) {
        Orders orders = new Orders();
        orders.setId(id);
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(orders);
    }


}
