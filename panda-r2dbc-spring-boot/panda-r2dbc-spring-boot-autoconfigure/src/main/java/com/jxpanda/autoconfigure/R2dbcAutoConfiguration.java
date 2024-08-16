package com.jxpanda.autoconfigure;

import com.jxpanda.r2dbc.spring.data.config.R2dbcConfigProperties;
import com.jxpanda.r2dbc.spring.data.core.R2dbcEntityTemplateAdapter;
import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.convert.MappingReactiveConverter;
import com.jxpanda.r2dbc.spring.data.core.enhance.handler.R2dbcCustomTypeHandlers;
import com.jxpanda.r2dbc.spring.data.core.enhance.key.AbstractSnowflakeGenerator;
import com.jxpanda.r2dbc.spring.data.core.enhance.key.IdGenerator;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2DbcLogicDeletePlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcPluginExecutor;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcPluginName;
import com.jxpanda.r2dbc.spring.data.dialect.DialectResolver;
import com.jxpanda.r2dbc.spring.data.infrastructure.constant.StringConstant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;
import org.springframework.data.relational.core.mapping.DefaultNamingStrategy;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.r2dbc.core.DatabaseClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * @author Panda
 */
@AutoConfiguration(after = org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration.class)
@EnableConfigurationProperties(R2dbcProperties.class)
@ComponentScan(basePackages = {"com.jxpanda.r2dbc.spring.data.config", "com.jxpanda.r2dbc.spring.data.core.kit"})
public class R2dbcAutoConfiguration {

    private final DatabaseClient databaseClient;

    private final R2dbcDialect r2dbcDialect;

    private final R2dbcProperties r2dbcProperties;


    public R2dbcAutoConfiguration(DatabaseClient databaseClient, R2dbcProperties r2dbcProperties) {
        this.databaseClient = databaseClient;
        this.r2dbcDialect = r2dbcDialect(databaseClient);
        this.r2dbcProperties = r2dbcProperties;
    }

    @Bean
    public R2dbcDialect r2dbcDialect(DatabaseClient databaseClient) {
        return DialectResolver.getDialect(databaseClient.getConnectionFactory());
    }

    @Bean
    public R2dbcConfigProperties r2dbcConfigProperties(R2dbcProperties r2dbcProperties) {
        return r2dbcProperties.transfer();
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveEntityTemplate reactiveEntityTemplate(MappingReactiveConverter r2dbcConverter) {
        return new ReactiveEntityTemplate(this.databaseClient, this.r2dbcDialect, r2dbcConverter);
    }

    @Bean
    public R2dbcEntityTemplate r2dbcEntityTemplate(ReactiveEntityTemplate reactiveEntityTemplate) {
        return new R2dbcEntityTemplateAdapter(reactiveEntityTemplate);
    }


    @Bean
    @ConditionalOnMissingBean
    public MappingReactiveConverter r2dbcConverter(R2dbcMappingContext mappingContext,
                                                   R2dbcCustomConversions r2dbcCustomConversions,
                                                   R2dbcCustomTypeHandlers r2dbcCustomTypeHandlers,
                                                   NamingStrategy namingStrategy) {
        return new MappingReactiveConverter(mappingContext, r2dbcCustomConversions, r2dbcCustomTypeHandlers, namingStrategy);
    }


    @Bean
    @ConditionalOnMissingBean
    public R2dbcMappingContext r2dbcMappingContext(ObjectProvider<NamingStrategy> namingStrategy,
                                                   R2dbcCustomConversions r2dbcCustomConversions) {
        R2dbcMappingContext relationalMappingContext = new R2dbcMappingContext(
                namingStrategy.getIfAvailable(() -> DefaultNamingStrategy.INSTANCE));
        relationalMappingContext.setForceQuote(this.r2dbcProperties.database().forceQuote());
        relationalMappingContext.setSimpleTypeHolder(r2dbcCustomConversions.getSimpleTypeHolder());
        return relationalMappingContext;
    }

    @Bean
    @ConditionalOnMissingBean
    public NamingStrategy namingStrategy() {
        return this.r2dbcProperties.mapping().namingStrategy();
    }


    @Bean
    @ConditionalOnMissingBean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Object> converters = new ArrayList<>(this.r2dbcDialect.getConverters());
        converters.addAll(R2dbcCustomConversions.STORE_CONVERTERS);
        return new R2dbcCustomConversions(
                CustomConversions.StoreConversions.of(this.r2dbcDialect.getSimpleTypeHolder(), converters),
                Collections.emptyList());
    }

    @Bean
    @ConditionalOnMissingBean
    public R2dbcCustomTypeHandlers r2dbcCustomTypeHandlers() {
        return new R2dbcCustomTypeHandlers();
    }

    @Bean
    @ConditionalOnMissingBean
    public R2dbcPluginExecutor r2dbcPluginExecutor() {
        return new R2dbcPluginExecutor()
                .addPlugin(new R2DbcLogicDeletePlugin());
    }


    @Bean
    @ConditionalOnMissingBean
    public IdGenerator<?> idGenerator() {

        return new AbstractSnowflakeGenerator<String>(this.r2dbcProperties.database().dataCenterId(), this.r2dbcProperties.database().workerId()) {
            @Override
            protected String cast(Long id) {
                return id.toString();
            }

            @Override
            public boolean isIdEffective(String id) {
                return !StringConstant.ID_DEFAULT_VALUES.contains(id);
            }
        };

    }


}
