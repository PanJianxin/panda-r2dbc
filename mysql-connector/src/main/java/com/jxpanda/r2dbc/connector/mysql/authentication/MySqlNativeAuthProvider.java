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

package com.jxpanda.r2dbc.connector.mysql.authentication;

import com.jxpanda.r2dbc.connector.mysql.collation.CharCollation;
import reactor.util.annotation.Nullable;

import static com.jxpanda.r2dbc.connector.mysql.util.AssertUtils.requireNonNull;
import static com.jxpanda.r2dbc.connector.mysql.util.InternalArrays.EMPTY_BYTES;

/**
 * An implementation of {@link MySqlAuthProvider} for type "mysql_native_password".
 */
final class MySqlNativeAuthProvider implements MySqlAuthProvider {

    static final MySqlNativeAuthProvider INSTANCE = new MySqlNativeAuthProvider();

    private static final String ALGORITHM = "SHA-1";

    private static final boolean IS_LEFT_SALT = true;

    @Override
    public boolean isSslNecessary() {
        return false;
    }

    /**
     * SHA1(password) `all bytes xor` SHA1( salt + SHA1( SHA1(password) ) )
     * <p>
     * {@inheritDoc}
     */
    @Override
    public byte[] authentication(@Nullable CharSequence password, byte[] salt, CharCollation collation) {
        if (password == null || password.length() <= 0) {
            return EMPTY_BYTES;
        }

        requireNonNull(salt, "salt must not be null when password exists");
        requireNonNull(collation, "collation must not be null when password exists");

        return AuthUtils.hash(ALGORITHM, IS_LEFT_SALT, password, salt, collation.getCharset());
    }

    @Override
    public MySqlAuthProvider next() {
        return this;
    }

    @Override
    public String getType() {
        return MYSQL_NATIVE_PASSWORD;
    }

    private MySqlNativeAuthProvider() { }
}
