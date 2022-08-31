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
import com.jxpanda.r2dbc.spring.data.convert.MappingR2dbcConverter;
import com.jxpanda.r2dbc.spring.data.convert.R2dbcConverter;
import com.jxpanda.r2dbc.spring.data.convert.R2dbcCustomConversions;
import com.jxpanda.r2dbc.spring.data.convert.R2dbcCustomTypeHandlers;
import com.jxpanda.r2dbc.spring.data.core.R2dbcEntityTemplate;
import com.jxpanda.r2dbc.spring.data.dialect.DialectResolver;
import com.jxpanda.r2dbc.spring.data.dialect.R2dbcDialect;
import com.jxpanda.r2dbc.spring.data.mapping.R2dbcMappingContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.data.convert.CustomConversions;
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
    public R2dbcEntityTemplate r2dbcEntityTemplate(R2dbcConverter r2dbcConverter) {
        return new R2dbcEntityTemplate(this.databaseClient, this.dialect, r2dbcConverter);
    }

    @Bean
    @ConditionalOnMissingBean
    public R2dbcMappingContext r2dbcMappingContext(ObjectProvider<NamingStrategy> namingStrategy,
                                                   R2dbcCustomConversions r2dbcCustomConversions) {
        R2dbcMappingContext relationalMappingContext = new R2dbcMappingContext(
                namingStrategy.getIfAvailable(() -> NamingStrategy.INSTANCE));
        relationalMappingContext.setForceQuote(r2dbcProperties.getMapping().isForceQuote());
        relationalMappingContext.setSimpleTypeHolder(r2dbcCustomConversions.getSimpleTypeHolder());
        return relationalMappingContext;
    }

    @Bean
    @ConditionalOnMissingBean
    public R2dbcConverter r2dbcConverter(R2dbcMappingContext mappingContext,
                                         R2dbcCustomConversions r2dbcCustomConversions,
                                         R2dbcCustomTypeHandlers r2dbcCustomTypeHandlers) {
        return new MappingR2dbcConverter(mappingContext, r2dbcCustomConversions, r2dbcCustomTypeHandlers);
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

}
