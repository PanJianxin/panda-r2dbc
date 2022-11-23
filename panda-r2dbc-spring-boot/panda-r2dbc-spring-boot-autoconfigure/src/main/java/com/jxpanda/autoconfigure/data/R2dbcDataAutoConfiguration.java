/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jxpanda.autoconfigure.data;

import com.jxpanda.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import com.jxpanda.autoconfigure.r2dbc.R2dbcProperties;
import com.jxpanda.r2dbc.spring.data.convert.MappingReactiveConverter;
import com.jxpanda.r2dbc.spring.data.convert.R2dbcCustomTypeHandlers;
import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.extension.constant.StringConstant;
import com.jxpanda.r2dbc.spring.data.extension.support.IdGenerator;
import com.jxpanda.r2dbc.spring.data.extension.support.SnowflakeIdGenerator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;
import org.springframework.data.relational.core.mapping.DefaultNamingStrategy;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.r2dbc.core.DatabaseClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link DatabaseClient}.
 *
 * @author Mark Paluch
 * @author Oliver Drotbohm
 * @since 2.3.0
 */
@AutoConfiguration(after = R2dbcAutoConfiguration.class)
@ConditionalOnClass({DatabaseClient.class, R2dbcEntityTemplate.class})
@ConditionalOnSingleCandidate(DatabaseClient.class)
public class R2dbcDataAutoConfiguration {

    private final DatabaseClient databaseClient;

    private final R2dbcDialect dialect;

    private final R2dbcProperties r2dbcProperties;


    public R2dbcDataAutoConfiguration(DatabaseClient databaseClient, R2dbcProperties r2dbcProperties) {
        this.databaseClient = databaseClient;
        this.dialect = DialectResolver.getDialect(this.databaseClient.getConnectionFactory());
        this.r2dbcProperties = r2dbcProperties;
    }


    @Bean
    @ConditionalOnMissingBean
    public MappingReactiveConverter r2dbcConverter(R2dbcMappingContext mappingContext,
                                                   R2dbcCustomConversions r2dbcCustomConversions,
                                                   R2dbcCustomTypeHandlers r2dbcCustomTypeHandlers) {
        return new MappingReactiveConverter(mappingContext, r2dbcCustomConversions, r2dbcCustomTypeHandlers, r2dbcProperties.getMappingProperties().transfer());
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveEntityTemplate r2dbcEntityTemplate(MappingReactiveConverter r2dbcConverter) {
        return new ReactiveEntityTemplate(this.databaseClient, this.dialect, r2dbcConverter);
    }

    @Bean
    @ConditionalOnMissingBean
    public R2dbcMappingContext r2dbcMappingContext(ObjectProvider<NamingStrategy> namingStrategy,
                                                   R2dbcCustomConversions r2dbcCustomConversions) {
        R2dbcMappingContext relationalMappingContext = new R2dbcMappingContext(
                namingStrategy.getIfAvailable(() -> DefaultNamingStrategy.INSTANCE));
        relationalMappingContext.setForceQuote(r2dbcProperties.getMappingProperties().isForceQuote());
        relationalMappingContext.setSimpleTypeHolder(r2dbcCustomConversions.getSimpleTypeHolder());
        return relationalMappingContext;
    }

    @Bean
    @ConditionalOnMissingBean
    public NamingStrategy namingStrategy() {
        return r2dbcProperties.getMappingProperties().getNamingPolicy();
    }


    @Bean
    @ConditionalOnMissingBean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Object> converters = new ArrayList<>(this.dialect.getConverters());
        converters.addAll(R2dbcCustomConversions.STORE_CONVERTERS);
        return new R2dbcCustomConversions(
                CustomConversions.StoreConversions.of(this.dialect.getSimpleTypeHolder(), converters),
                Collections.emptyList());
    }

    @Bean
    @ConditionalOnMissingBean
    public R2dbcCustomTypeHandlers r2dbcCustomTypeHandlers() {
        return new R2dbcCustomTypeHandlers();
    }


    @Bean
    @ConditionalOnMissingBean
    public IdGenerator<?> idGenerator(R2dbcProperties r2dbcProperties) {
        R2dbcProperties.MappingProperties mappingProperties = r2dbcProperties.getMappingProperties();
        return new SnowflakeIdGenerator<String>(mappingProperties.getDataCenterId(), mappingProperties.getWorkerId()) {
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
