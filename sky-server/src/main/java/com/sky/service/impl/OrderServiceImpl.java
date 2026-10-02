package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
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
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

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
    private AddressBookMapper addressBookMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

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
    @Transactional
    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) {
        String orderNumber = ordersPaymentDTO.getOrderNumber();

        Orders orders = getOwnedOrder(orderMapper.getByNumber(orderNumber).getId());

        // 验证订单状态是否为待支付
        if (!Orders.PENDING_PAYMENT.equals(orders.getStatus())) {
            log.warn("订单 {} 状态不是待支付状态 {}", orders.getNumber(), orders.getStatus());
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", "ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        orderMapper.updateStatus(Orders.TO_BE_CONFIRMED, Orders.PAID, LocalDateTime.now(), orders.getId());

        //通过WebSocket向客户端浏览器推送消息 type orderId content
        Map<String, Object> map = new HashMap<>();
        map.put("type", 1);  //1表示来单提醒 2表示客户催单
        map.put("orderId", orders.getId());
        map.put("content", "订单号：" + ordersPaymentDTO.getOrderNumber());

        webSocketServer.sendToAllClient(JSON.toJSONString(map));
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
        if (orders.getStatus().equals(Orders.PENDING_PAYMENT) || orders.getStatus().equals(Orders.TO_BE_CONFIRMED) || orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS) || orders.getStatus().equals(Orders.COMPLETED)) {
            orders.setId(ordersCancelDTO.getId());
            orders.setCancelReason(ordersCancelDTO.getCancelReason());
            orders.setCancelTime(LocalDateTime.now());
            orders.setStatus(Orders.CANCELLED);
            orderMapper.update(orders);
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
        Orders orders = getOwnedOrder(id);

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

    /**
     * 获取订单列表
     *
     * @param page     页码
     * @param pageSize 页大小
     * @param status   订单状态
     * @return 订单列表
     */
    @Override
    public PageResult historyOrders(int page, int pageSize, Integer status) {
        PageHelper.startPage(page, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        Page<Orders> pageResult = orderMapper.historyOrders(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();

        // 查询出订单明细，并封装入OrderVO进行响应
        if (pageResult != null && pageResult.getTotal() > 0) {
            for (Orders orders : pageResult) {
                Long orderId = orders.getId();// 订单id

                // 查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }

        if (pageResult == null) {
            throw new OrderBusinessException(MessageConstant.UNKNOWN_ERROR);
        }

        return new PageResult(pageResult.getTotal(), list);
    }

    /**
     * 根据id查询订单
     *
     * @param id 订单id
     * @return 订单详情
     */
    @Override
    public OrderVO orderDetail(Long id) {
        Orders order = getOwnedOrder(id);

        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(order.getId());

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        orderVO.setOrderDetailList(orderDetails);
        return orderVO;
    }

    /**
     * 用户取消订单
     *
     * @param id 订单id
     */
    @Override
    public void userCancel(Long id) {
        Orders order = getOwnedOrder(id);

        order.setStatus(Orders.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason("用户取消");

        orderMapper.update(order);
    }

    /**
     * 再来一单
     *
     * @param id 订单id
     */
    @Override
    @Transactional
    public void repetition(Long id) {
        Orders order = getOwnedOrder(id);
        Long oldOrderId = order.getId();

        // 对Orders表进行更新，设置订单状态为待支付
        order.setId(null);
        // TODO 并发重复问题
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setOrderTime(LocalDateTime.now());
        order.setCheckoutTime(null);
        order.setPayStatus(Orders.UN_PAID);
        order.setCancelReason(null);
        order.setRejectionReason(null);
        order.setCancelTime(null);
        order.setDeliveryTime(null);
        order.setStatus(Orders.PENDING_PAYMENT);

        orderMapper.insert(order);

        Long newOrderId = order.getId();
        // 对orders_details表进行更新
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(oldOrderId);

        List<OrderDetail> orderDetailList = new ArrayList<>();
        // 向订单明细表插入数据
        for (OrderDetail orderDetail : orderDetails) {
            OrderDetail newOrderDetail = new OrderDetail();
            BeanUtils.copyProperties(orderDetail, newOrderDetail);
            newOrderDetail.setOrderId(newOrderId);
            orderDetailList.add(newOrderDetail);
        }

        orderDetailMapper.insertBatch(orderDetailList);
    }

    /**
     * 订单提醒
     *
     * @param id 订单id
     */
    @Override
    public void reminder(Long id) {
        Orders order = orderMapper.getById(id);

        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("type", 2); // 2表示订单提醒
        map.put("orderId", order.getId());
        map.put("content", "订单号：" + order.getNumber());
        String json = JSON.toJSONString(map);

        webSocketServer.sendToAllClient(json);
    }

    /**
     * 校验订单存在且归属当前登录用户,返回订单本身
     *
     * @param id 订单id
     * @return 订单对象
     * @throws OrderBusinessException 订单不存在或归属用户不匹配
     */
    private Orders getOwnedOrder(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!BaseContext.getCurrentId().equals(orders.getUserId())) {
            log.warn("用户 {} 越权访问订单 {}", BaseContext.getCurrentId(), id);
            throw new OrderBusinessException(MessageConstant.USER_INFO_MISMATCH);
        }
        return orders;
    }
}
