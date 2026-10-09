package com.sky.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SnowflakeConfig {

    /**
     * workerId 机器ID 0~31
     * dataCenterId 数据中心ID 0~31
     * 多服务器部署时，每台机器配置不同workerId，防止ID重复
     */
    @Value("${sky.snowflake.worker-id:1}")
    private long workerId;
    @Value("${sky.snowflake.data-center-id:0}")
    private long dataCenterId;

    @Bean
    public Snowflake snowflake() {
        return IdUtil.getSnowflake(workerId, dataCenterId);
    }
}
