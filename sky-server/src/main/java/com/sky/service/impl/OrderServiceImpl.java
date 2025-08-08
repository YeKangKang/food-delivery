package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(BaseContext.getCurrentId()); // 按照 userId 查找
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
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
        orders.setAddress(ab.getProvinceName()
                            + ab.getCityName()
                            + ab.getDistrictName()
                            + ab.getDetail());                          // TODO 可能需要加地址，不然后续订单详情功能为null
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

    /**
     * 历史订单分页查询
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    @Override
    public PageResult page(Integer page, Integer pageSize, Integer status) {
        // 设置 PageHelper 自动在SQL语句后插入LIMIT分页
        PageHelper.startPage(page, pageSize);

        // 1. 根据 用户id+status（如果提供） 查询所有订单
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setStatus(status);   // 要查询的订单状态
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());   // 要查询的用户id

        // 分页查询 order
        Page<Orders> ordersPage = orderMapper.pageQuery(ordersPageQueryDTO);

        // 2. 根据 订单id 查询当前订单对应的所有商品，并封装成 OrderVO
        List<OrderVO> orderVOList = new ArrayList<>();   // ResultPage 的list部分

        if (ordersPage != null && !ordersPage.isEmpty()) {
            for (Orders orders : ordersPage) {
                // 查询和这个 order 的 id 对应的 OrderDetail 数据
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

                // 封装 Order 和 OrderDetail
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetailList);

                orderVOList.add(orderVO);
            }
        }

//        // 使用 Map 优化N+1查询的方法
//        List<OrderVO> orderVOList = new ArrayList<>();
//        if (ordersPage != null && !ordersPage.isEmpty()) {
//            orderVOList = buildOrderVOList(ordersPage);
//        }

        return new PageResult(ordersPage.getTotal(), orderVOList);
    }

    // TODO 注释后提交
    /**
     * 这是一个替换方法: 用于替换查询detail并封装orderVO
     * @param ordersPage    orders的分页对象
     */
    private List<OrderVO> buildOrderVOList(Page<Orders> ordersPage) {
        // 获取当前用户的所有 orderId
        // List<Long> orderIdList = orderMapper.getIdListByUserId(BaseContext.getCurrentId());
        List<Long> orderIdList = new ArrayList<>();
        for (Orders orders : ordersPage) {
            orderIdList.add(orders.getId());
        }

        // 根据 orderId 查询所有 OrderDetail
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderIdList(orderIdList);

        // 给 orderDetailList 按照 orderID 进行分类(Map(key:orderID, value:List<OrderDetail>))
        Map<Long, List<OrderDetail>> orderDetailMap = new HashMap<>();
        for (OrderDetail orderDetail : orderDetailList) {
            // 判断有没有 key=orderDetail.getOrderId()
            if (!orderDetailMap.containsKey(orderDetail.getOrderId())) {
                orderDetailMap.put(orderDetail.getOrderId(), new ArrayList<>());    // 如果没有就创建一个新的
            }
            orderDetailMap.get(orderDetail.getOrderId()).add(orderDetail);
        }

        // 按照分类拼装 OrderVO
        List<OrderVO> orderVOList = new ArrayList<>();
        for (Orders orders : ordersPage) {
            // 如果map中包含这个order的id作为key
            if (orderDetailMap.containsKey(orders.getId())) {
                // 封装结果
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetailMap.get(orders.getId())); // 获得该订单对应的所有菜品关系

                orderVOList.add(orderVO);
            }
        }

        return orderVOList;
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO details(Long id) {
        // 根据 订单id 查询特定订单数据
        Orders orders = orderMapper.getById(id);

        // 根据 订单id 查询订单关系表数据
        List<OrderDetail> orderDetail = orderDetailMapper.getByOrderId(orders.getId());

        // 封装并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);  // 拷贝 orders
        orderVO.setOrderDetailList(orderDetail);    // 拷贝 ordersDetailList

        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     * @return
     */
    @Override
    public void cancel(Long id) {
        // 判断该订单是否存在
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消 (只有商家还没有接单是用户才可以取消)
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 待接单状态下取消需要进行退款
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            // 向微信API发起退款请求（这里假设请求完成）
//            weChatPayUtil.refund(
//                    ordersDB.getNumber(),     //商户订单号
//                    ordersDB.getNumber(),     //商户退款单号
//                    new BigDecimal(0.01),     //退款金额，单位 元
//                    new BigDecimal(0.01));    //原订单金额

            orders.setPayStatus(Orders.REFUND); // 设置支付状态为退款
        }
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orders.setStatus(Orders.CANCELLED);
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     * @param id
     * @return
     */
    @Override
    public void repetition(Long id) {
        // 再来一单就是把商品重新加到购物车中

        // 根据 orderId 查询订单关系表
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单关系表数据复制到购物车对象中，补齐不全的值
        List<ShoppingCart> shoppingCartList = new ArrayList<>();
        if (orderDetailList != null && !orderDetailList.isEmpty()) {
            // 给每一条订单关系的记录转移到购物车对象
            for (OrderDetail orderDetail : orderDetailList) {
                ShoppingCart shoppingCart = new ShoppingCart();
                // 排除 id，防止主键冲突
                BeanUtils.copyProperties(orderDetail, shoppingCart, "id");
                shoppingCart.setUserId(BaseContext.getCurrentId());
                shoppingCart.setCreateTime(LocalDateTime.now());
                shoppingCartList.add(shoppingCart);
            }
        }

        // 将购物车对象列表插入购物车表
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 管理端订单分页搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        // 设置 PageHelper自动分页, 其会自动在 SQL 结尾添加 LIMIT 实现分页，还有单独发一条 COUNT(*)
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        // 准备封装数据：根据前端需求查询分页结果
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 准备封装数据
        List<OrderVO> orderVOList = new ArrayList<>();

        if (!CollectionUtils.isEmpty(page)) {   // 只有查到了order才执行

            // 准备封装数据：获得这些order对应的所有商品关系
            List<Long> orderIdList = new ArrayList<>();
            for (Orders orders : page) {
                orderIdList.add(orders.getId());
            }

            // 使用 Map 以orderID为key进行分组
            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderIdList(orderIdList);    // 获取的所有商品关系数据（未分组）
            Map<Long, List<OrderDetail>> orderDetailMap = new HashMap<>();
            for (OrderDetail orderDetail : orderDetailList) {
                if (!orderDetailMap.containsKey(orderDetail.getOrderId())) {
                    orderDetailMap.put(orderDetail.getOrderId(), new ArrayList<>());
                }
                orderDetailMap.get(orderDetail.getOrderId()).add(orderDetail);
            }

            // 封装数据:（OrderVO = Order + （String）orderDishes）
            for (Orders orders : page) {
                // 每个 order 取出和他们对应的商品关系数据封装
                if (orderDetailMap.containsKey(orders.getId())) {
                    OrderVO orderVO = new OrderVO();
                    BeanUtils.copyProperties(orders, orderVO);  // 拷贝 Order 数据到 orderVO
                    String orderDishes = getOrderDetailByStr(orderDetailMap.get(orders.getId()));
                    orderVO.setOrderDishes(orderDishes);
                    orderVOList.add(orderVO);
                }
            }
        }
        return new PageResult(page.getTotal(), orderVOList);
    }

    /**
     * 将该订单的所有商品信息拼接为字符串返回
     * @param orderDetailList
     * @return
     */
    private String getOrderDetailByStr(List<OrderDetail> orderDetailList) {
        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }


    @Override
    public OrderStatisticsVO statistics() {

        // 查询 待接单，待派送，派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);    // 待接单（2）
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);  // 待派送（3）
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);  // 派送中（4）

        // 准备返回对象
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);

        return orderStatisticsVO;
    }
}