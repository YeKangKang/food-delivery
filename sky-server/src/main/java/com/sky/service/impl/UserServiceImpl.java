package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private UserMapper userMapper;

    /**
     * 微信用户登陆功能
     * - 使用用户提供的一次性授权码通过 HttpClient 请求微信API
     * @param userLoginDTO
     * @return
     */
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        // 调用微信接口服务，获取当前用户的 OpenID
        String openId = getOpenId(userLoginDTO.getCode());

        // 如果 OpenID 为空，抛出异常（微信查询不到该登陆者）
        if (openId == null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        // 判断用户是不是新用户（查找自己的数据库），如果不是就返回 User，是就注册到数据库再返回 User
        User user = userMapper.getByOpenId(openId); // 通过 openid 查询是否有这个用户（是否注册）
        // 判断是不是新用户
        if (user == null) {
            // 新用户，注册，并向数据库中插入值
            user = User.builder()
                    .openid(openId)
                    .createTime(LocalDateTime.now())
                    .build();

            userMapper.insert(user);
        }
        return user;
    }

    /**
     * 该方法使用前端用户的一次性授权码向微信API发送登陆请求，来获得这个用户的OpenId
     * @param code
     * @return
     */
    private String getOpenId(String code) {
        // 微信API的登陆接口需要4个参数，这里采用一个自己构建的 HttpClient 工具类向微信服务器发送请求
        Map<String, String> paramMap = new HashMap<>(); // 微信官方需要这4个参数作为请求参数
        paramMap.put("appid", weChatProperties.getAppid());
        paramMap.put("secret", weChatProperties.getSecret());
        paramMap.put("js_code", code);
        paramMap.put("grant_type", "authorization_code");
        // 将包含请求参数的 map 传入工具类，微信API会返回一个JSON风格的字符串
        String json = HttpClientUtil.doGet(WX_LOGIN, paramMap);

        // 字符串是一个 JSON 风格的，使用JSON工具类进行处理成JavaJSON对象
        JSONObject jsonObject = JSON.parseObject(json);
        return jsonObject.getString("openid");  // 获取 openId 的值
    }
}
