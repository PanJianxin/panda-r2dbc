package com.jxpanda.r2dbc.spring.data.extension.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.logging.LogLevel;


@Getter
@Setter
@ConfigurationProperties(prefix = ProxyProperties.PREFIX)
public class ProxyProperties {

    public static final String PREFIX = "panda.r2dbc.proxy";

    private boolean enable = false;

    private LogLevel logLevel = LogLevel.INFO;

}
