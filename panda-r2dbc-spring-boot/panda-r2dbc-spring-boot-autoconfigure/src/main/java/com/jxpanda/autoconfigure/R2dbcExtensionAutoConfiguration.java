package com.jxpanda.autoconfigure;


import com.jxpanda.autoconfigure.configure.ProxyConfigure;
import com.jxpanda.r2dbc.spring.data.extension.component.CrudEntityRegistrar;
import com.jxpanda.r2dbc.spring.data.extension.config.properties.ExtensionProperties;
import com.jxpanda.r2dbc.spring.data.extension.config.properties.ProxyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;


@AutoConfigureAfter({R2dbcAutoConfiguration.class})
@EnableConfigurationProperties({ExtensionProperties.class, ProxyProperties.class})
@ImportAutoConfiguration({ProxyConfigure.class})
@Import(CrudEntityRegistrar.class)
@RequiredArgsConstructor
public class R2dbcExtensionAutoConfiguration {


}
