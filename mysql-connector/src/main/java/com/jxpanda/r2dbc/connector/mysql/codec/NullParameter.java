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

package com.jxpanda.r2dbc.connector.mysql.codec;

import com.jxpanda.r2dbc.connector.mysql.Parameter;
import com.jxpanda.r2dbc.connector.mysql.ParameterWriter;
import com.jxpanda.r2dbc.connector.mysql.constant.MySqlType;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Mono;

/**
 * An implementation of {@link Parameter} which considers value is {@code null}.
 * <p>
 * Note: the parameter is marked with a bitmap of {@code null}, so {@link #publishBinary} will not do
 * anything.
 */
final class NullParameter implements Parameter {

    static final NullParameter INSTANCE = new NullParameter();

    @Override
    public boolean isNull() {
        return true;
    }

    /**
     * Binary protocol encode null parameter to empty.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public Mono<ByteBuf> publishBinary() {
        return Mono.empty();
    }

    @Override
    public Mono<Void> publishText(ParameterWriter writer) {
        return Mono.fromRunnable(writer::writeNull);
    }

    @Override
    public MySqlType getType() {
        return MySqlType.NULL;
    }

    @Override
    public void dispose() {
        // No resource to release.
    }

    @Override
    public String toString() {
        // Hide parameter detail even it is null.
        return "Parameter{REDACTED}";
    }

    private NullParameter() { }
}
