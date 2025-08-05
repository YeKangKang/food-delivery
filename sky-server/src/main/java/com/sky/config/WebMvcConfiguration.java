package com.sky.config;

import com.sky.interceptor.JwtTokenAdminInterceptor;
import com.sky.interceptor.JwtTokenUserInterceptor;
import com.sky.json.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.List;

/**
 * 配置类，注册web层相关组件
 */
@Configuration
@Slf4j
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;  // 管理端拦截器

    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;    // 用户端拦截器

    /**
     * 注册自定义拦截器
     *
     * @param registry
     */
    protected void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        // 管理端拦截器
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")   // 拦截所有访问 “/admin/” 开头的请求
                .excludePathPatterns("/admin/employee/login");  // 登陆请求不拦截
        // 用户端拦截器
        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/user/**") // 拦截所有访问 “/user/” 开头的请求
                .excludePathPatterns("/user/user/login")    // 登陆请求不拦截
                .excludePathPatterns("/user/shop/status");  // 查看店铺营业状态不拦截
    }

    /**
     * 扫描 管理端 Controller 包，通过knife4j生成接口文档
     * @return
     */
    @Bean
    public Docket docketShop() {
        log.info("准备生成接口文档...");
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("苍穹外卖项目接口文档")
                .version("2.0")
                .description("苍穹外卖项目接口文档")
                .build();

        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .groupName("管理端接口")
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.sky.controller.admin"))    // 扫描接口类所在的包
                .paths(PathSelectors.any())
                .build();
        return docket;
    }

    /**
     * 扫描 用户端 Controller 包，通过knife4j生成接口文档
     * @return
     */
    @Bean
    public Docket docketUser() {
        log.info("准备生成接口文档...");
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("苍穹外卖项目接口文档")
                .version("2.0")
                .description("苍穹外卖项目接口文档")
                .build();

        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .groupName("用户端接口")
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.sky.controller.user"))    // 扫描接口类所在的包
                .paths(PathSelectors.any())
                .build();
        return docket;
    }

    /**
     * 设置静态资源映射
     * @param registry
     */
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始设置静态资源映射...");
        registry.addResourceHandler("/doc.html") // 映射 Knife4j 主页面
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**") // 映射 Knife4j 的前端依赖资源（CSS等）
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * 扩展 Spring MVC 框架的原有消息转换器
     * @param converters
     */
    @Override
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 创建一个消息转换器对象
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        // 为消息转换器对象设置一个对象映射器，以实现Java对象和JSON数据的序列化/反序列化
        mappingJackson2HttpMessageConverter.setObjectMapper(new JacksonObjectMapper());
        // 将消息转换器加入 Spring MVC 消息转换器列表中完成扩展
        // (索引：0，表示消息转换器列表中第一个使用)
        converters.add(0, mappingJackson2HttpMessageConverter);
    }
}
