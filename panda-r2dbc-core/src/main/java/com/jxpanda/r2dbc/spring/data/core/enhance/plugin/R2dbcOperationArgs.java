package com.jxpanda.r2dbc.spring.data.core.enhance.plugin;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.SqlIdentifier;

@Getter
@Builder
public class R2dbcOperationArgs<T, R> {
    private Query query;
    private T entity;
    private Class<T> entityClass;
    private Class<R> returnType;
    private SqlIdentifier tableName;
    private OutboundRow outboundRow;
}
