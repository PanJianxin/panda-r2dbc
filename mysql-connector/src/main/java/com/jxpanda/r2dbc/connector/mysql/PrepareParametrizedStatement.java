/*
 * Copyright 2018-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jxpanda.r2dbc.connector.mysql;

import com.jxpanda.r2dbc.connector.mysql.cache.PrepareCache;
import com.jxpanda.r2dbc.connector.mysql.client.Client;
import com.jxpanda.r2dbc.connector.mysql.codec.Codecs;
import reactor.core.publisher.Flux;

import java.util.List;

import static com.jxpanda.r2dbc.connector.mysql.util.AssertUtils.require;

/**
 * An implementation of {@link ParametrizedStatementSupport} based on MySQL prepare query.
 */
final class PrepareParametrizedStatement extends ParametrizedStatementSupport {

    private final PrepareCache prepareCache;

    private int fetchSize = 0;

    PrepareParametrizedStatement(Client client, Codecs codecs, Query query, ConnectionContext context,
        PrepareCache prepareCache) {
        super(client, codecs, query, context);
        this.prepareCache = prepareCache;
    }

    @Override
    public Flux<MySqlResult> execute(List<Binding> bindings) {
        return QueryFlow.execute(client, query.getFormattedSql(), bindings, fetchSize, prepareCache)
            .map(messages -> MySqlResult.toResult(true, codecs, context, generatedKeyName, messages));
    }

    @Override
    public MySqlStatement fetchSize(int rows) {
        require(rows >= 0, "Fetch size must be greater or equal to zero");

        this.fetchSize = rows;
        return this;
    }
}
