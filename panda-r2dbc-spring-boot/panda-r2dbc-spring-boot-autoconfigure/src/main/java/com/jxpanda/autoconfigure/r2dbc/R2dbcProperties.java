/*
 * Copyright 2012-2021 the original author or authors.
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

package com.jxpanda.autoconfigure.r2dbc;

import com.jxpanda.r2dbc.spring.data.config.R2dbcMappingProperties;
import com.jxpanda.r2dbc.spring.data.extension.policy.NamingPolicy;
import com.jxpanda.r2dbc.spring.data.extension.policy.ValidationPolicy;
import io.r2dbc.spi.ValidationDepth;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Configuration properties for R2DBC.
 *
 * @author Mark Paluch
 * @author Andreas Killaitis
 * @author Stephane Nicoll
 * @author Rodolpho S. Couto
 * @since 2.3.0
 */
@Data
@ConfigurationProperties(prefix = "panda.r2dbc")
public class R2dbcProperties {

    /**
     * Database name. Set if no name is specified in the url. Default to "testdb" when
     * using an embedded database.
     */
    private String name;

    /**
     * Whether to generate a random database name. Ignore any configured name when
     * enabled.
     */
    private boolean generateUniqueName;

    /**
     * R2DBC URL of the database. database name, username, password and pooling options
     * specified in the url take precedence over individual options.
     */
    private String url;

    /**
     * Login username of the database. Set if no username is specified in the url.
     */
    private String username;

    /**
     * Login password of the database. Set if no password is specified in the url.
     */
    private String password;

    /**
     * Additional R2DBC options.
     */
    private final Map<String, String> properties = new LinkedHashMap<>();

    /**
     * 映射关系相关配置
     */
    private final MappingProperties mappingProperties = new MappingProperties();

    /**
     * pool config
     */
    private final Pool pool = new Pool();

    private String uniqueName;


    /**
     * Provide a unique name specific to this instance. Calling this method several times
     * return the same unique name.
     *
     * @return a unique name for this instance
     */
    public String determineUniqueName() {
        if (this.uniqueName == null) {
            this.uniqueName = UUID.randomUUID().toString();
        }
        return this.uniqueName;
    }

    @Data
    public static class Pool {

        /**
         * Maximum amount of time that a connection is allowed to sit idle in the pool.
         */
        private Duration maxIdleTime = Duration.ofMinutes(30);

        /**
         * Maximum lifetime of a connection in the pool. By default, connections have an
         * infinite lifetime.
         */
        private Duration maxLifeTime;

        /**
         * Maximum time to acquire a connection from the pool. By default, wait
         * indefinitely.
         */
        private Duration maxAcquireTime;

        /**
         * Maximum time to wait to create a new connection. By default, wait indefinitely.
         */
        private Duration maxCreateConnectionTime;

        /**
         * Initial connection pool size.
         */
        private int initialSize = 10;

        /**
         * Maximal connection pool size.
         */
        private int maxSize = 10;

        /**
         * Validation query.
         */
        private String validationQuery;

        /**
         * Validation depth.
         */
        private ValidationDepth validationDepth = ValidationDepth.LOCAL;

        /**
         * Whether pooling is enabled. Requires r2dbc-pool.
         */
        private boolean enabled = true;

    }


    @Data
    public static class MappingProperties {

        /**
         * 是否在对象映射的过程中强制加上引用符
         * 例如：在MySQL中，使用反引号'`'来做引用标识符，则注入SQL：SELECT XXX FROM order 会变为 SELECT XXX FROM `order`
         * 使用此配置可以一定程度上避免SQL语句中出现关键字而导致的BAD SQL错误
         * 默认是true
         */
        private boolean forceQuote = true;

        /**
         * 数据中心ID，影响雪花算法ID生成规则，如果不使用雪花算法，这个配置不生效
         */
        private int dataCenterId = 0;

        /**
         * 工作节点ID，影响雪花算法ID生成规则，如果不使用雪花算法，这个配置不生效
         */
        private int workerId = 0;

        /**
         * 如果配置了任何一个逻辑删除值，则全局开启逻辑删除
         * 逻辑删除「正常值」的标记
         */
        private String logicNormalValue = "";
        /**
         * 如果配置了任何一个逻辑删除值，则全局开启逻辑删除
         * 逻辑删除「删除值」的标记
         */
        private String logicDeleteValue = "";

        /**
         * 字段命名策略，默认是不处理
         */
        private NamingPolicy namingPolicy = NamingPolicy.DEFAULT;

        /**
         * 字段验证策略
         * 优先级列表：字段注解（@TableColumn） > 类注解（@TableEntity） > 全局配置（R2dbcProperty）
         */
        private ValidationPolicy validationPolicy = ValidationPolicy.NOT_NULL;


        public R2dbcMappingProperties transfer() {
            return new R2dbcMappingProperties(forceQuote, dataCenterId, workerId, logicNormalValue, logicDeleteValue, namingPolicy, validationPolicy);
        }

    }

}
