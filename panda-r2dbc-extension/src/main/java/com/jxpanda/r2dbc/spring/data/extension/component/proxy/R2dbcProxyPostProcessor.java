package com.jxpanda.r2dbc.spring.data.extension.component.proxy;

import com.jxpanda.r2dbc.spring.data.extension.config.properties.ProxyProperties;
import io.r2dbc.proxy.ProxyConnectionFactory;
import io.r2dbc.proxy.callback.ProxyConfig;
import io.r2dbc.proxy.core.*;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.proxy.support.QueryExecutionInfoFormatter;
import io.r2dbc.spi.Blob;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.slf4j.event.Level;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.NonNull;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;

@Slf4j
@RequiredArgsConstructor
public class R2dbcProxyPostProcessor implements BeanPostProcessor {

    private final ProxyProperties proxyProperties;

    private final ProxyExecutionListener listener = new ProxyExecutionListener() {
        @Override
        public void beforeQuery(@NonNull QueryExecutionInfo info) {
            String fullSql = new QueryExecutionInfoFormatter()
                    .addConsumer(fullSqlConsumer())
                    .format(info);
            log.atLevel(Level.valueOf(proxyProperties.getLogLevel().name()))
                    .log("👉 Executed SQL: {}", fullSql);
        }
    };

    private final ProxyConfig proxyConfig = ProxyConfig.builder()
            .listener(listener)
            .build();


    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) {
        if (bean instanceof ConnectionFactory original) {
            return ProxyConnectionFactory.builder(original)
                    .proxyConfig(proxyConfig)
                    .build();
        }
        return bean;
    }

    private BiConsumer<QueryExecutionInfo, StringBuilder> fullSqlConsumer() {
        return (info, sb) -> {
            if (info.getQueries().isEmpty()) {
                return;
            }
            info.getQueries().forEach(queryInfo -> {
                // 拿这条 QueryInfo 的所有绑定参数列表（通常只有一次 execute）
                String query = queryInfo.getQuery();
                // 把所有的 indexBindings 按出现顺序拉平成一个值流，然后用 reduce 把每个问号依次替换
                String fullSql = queryInfo.getBindingsList().stream()
                        .flatMap(bindings -> bindings.getIndexBindings().stream())
                        .map(binding -> renderValue(binding.getBoundValue()))
                        .reduce(query, (acc, param) ->
                                acc.replaceFirst("\\?", Matcher.quoteReplacement(param))
                        );
                sb.append(fullSql).append("\n");
            });
        };
    }


    // helper：把 BoundValue 渲染成 SQL 字面量
    private static String renderValue(BoundValue boundValue) {
        if (boundValue.isNull()) {
            return "NULL";
        }
        Object value = boundValue.getValue();

        if (value instanceof Parameter parameter) {
            value = parameter.getValue();
        }
        if (value instanceof Blob) {
            return "'<BLOB>'";
        }

        String valueString = Objects.requireNonNull(value).toString();
        return "'" + valueString + "'";
    }

}
