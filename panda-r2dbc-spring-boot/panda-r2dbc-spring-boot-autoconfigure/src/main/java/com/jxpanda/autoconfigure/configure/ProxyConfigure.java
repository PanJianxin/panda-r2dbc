package com.jxpanda.autoconfigure.configure;

import com.jxpanda.r2dbc.spring.data.extension.config.properties.ProxyProperties;
import com.jxpanda.r2dbc.spring.data.extension.component.proxy.R2dbcProxyPostProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;


@Slf4j
@RequiredArgsConstructor
public class ProxyConfigure {

    private final ProxyProperties properties;

    @Bean
    @ConditionalOnClass(name = "io.r2dbc.proxy.ProxyConnectionFactory")
    @ConditionalOnProperty(prefix = ProxyProperties.PREFIX, name = "enable", havingValue = "true")
    public BeanPostProcessor proxyPostProcessor() {
        return new R2dbcProxyPostProcessor(properties);
    }


}
