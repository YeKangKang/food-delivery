package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
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
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;    // 用不到这个，复制来的

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional  // 事务
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        // 处理业务异常【一般来说这部分前端处理好了,这个是防止用postman等工具直接传数据，导致报错】
        // 异常：地址簿找不到该用户数据
        AddressBook ab = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId()); // 查询地址簿有没有这条数据
        if ( ab == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        // 异常：购物车表找不到该用户商品数据
        ShoppingCart spc = ShoppingCart.builder().id(BaseContext.getCurrentId()).build(); // 构造一个查询 id 的实体类
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(spc);
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 1. 向订单表插入 1 条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);  // 属性拷贝，无法拷贝的需要手动设置
        orders.setOrderTime(LocalDateTime.now());                       // 订单创建时间
        orders.setPayStatus(Orders.UN_PAID);                            // 支付状态
        orders.setStatus(Orders.PENDING_PAYMENT);                       // 订单状态
        orders.setNumber(String.valueOf(System.currentTimeMillis()));   // 用户时间戳当订单号
        orders.setPhone(ab.getPhone());                                 // 手机号（从 addressbook 对象中取）
        orders.setConsignee(ab.getConsignee());                         // 收货人（从 addressbook 对象中取）
        orders.setUserId(BaseContext.getCurrentId());                   // 用户id

        orderMapper.insert(orders); // 生成的主键需要后续插入到订单明细表

        // 2. 向订单明细表插入 n 条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);    // 订单id没有拷贝
            orderDetail.setOrderId(orders.getId()); // 设置当前订单明细数据关联的订单id（利用主键回写）
            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetailList); // 批量插入明细数据

        // 3. 清空该用户的购物车数据
        shoppingCartMapper.deleteByUserId(BaseContext.getCurrentId());

        // 4. 封装 VO 结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderTime(orders.getOrderTime())
                .orderAmount(orders.getAmount())
                .build();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
//        // 当前登录用户id
//        Long userId = BaseContext.getCurrentId();
//        User user = userMapper.getById(userId);
//
//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }
//
//        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
//        vo.setPackageStr(jsonObject.getString("package"));

        // 获取订单号
        String orderNumber = ordersPaymentDTO.getOrderNumber();

        // 调用支付成功，修改订单状态（跳过微信支付步骤）
        this.paySuccess(orderNumber);

        return new OrderPaymentVO();
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
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
}
