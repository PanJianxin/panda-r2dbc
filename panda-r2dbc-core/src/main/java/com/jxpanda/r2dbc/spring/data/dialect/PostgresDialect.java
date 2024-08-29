package com.jxpanda.r2dbc.spring.data.dialect;

/**
 * @author Panda
 */
@SuppressWarnings("ALL")
public class PostgresDialect extends org.springframework.data.r2dbc.dialect.PostgresDialect {


    /**
     * Singleton instance.
     */
    public static final PostgresDialect INSTANCE = new PostgresDialect();

}
