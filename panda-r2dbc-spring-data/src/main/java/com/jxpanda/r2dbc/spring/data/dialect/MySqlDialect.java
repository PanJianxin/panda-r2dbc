/*
 * Copyright 2019-2022 the original author or authors.
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
package com.jxpanda.r2dbc.spring.data.dialect;

import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.dialect.ArrayColumns;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * An SQL dialect for MySQL.
 *
 * @author Mark Paluch
 * @author Jens Schauder
 */
public class MySqlDialect extends org.springframework.data.r2dbc.dialect.MySqlDialect
        implements R2dbcDialect {

    /**
     * Singleton instance.
     */
    public static final MySqlDialect INSTANCE = new MySqlDialect();


    @Override
    public ArrayColumns getArraySupport() {
        return MySqlArrayColumns.INSTANCE;
    }


    protected enum MySqlArrayColumns implements ArrayColumns {

        INSTANCE;

        /*
         * (non-Javadoc)
         * @see org.springframework.data.relational.core.dialect.ArrayColumns#isSupported()
         */
        @Override
        public boolean isSupported() {
            return true;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.relational.core.dialect.ArrayColumns#getArrayType(java.lang.Class)
         */
        @Override
        public Class<?> getArrayType(Class<?> userType) {

            Assert.notNull(userType, "Array component type must not be null");

            return ClassUtils.resolvePrimitiveIfNecessary(userType);
        }
    }

}
