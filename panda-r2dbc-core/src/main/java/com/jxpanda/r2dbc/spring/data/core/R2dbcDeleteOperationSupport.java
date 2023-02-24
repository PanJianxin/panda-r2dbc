/*
 * Copyright 2020-2022 the original author or authors.
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
package com.jxpanda.r2dbc.spring.data.core;

import com.jxpanda.r2dbc.spring.data.core.kit.MappingKit;
import com.jxpanda.r2dbc.spring.data.core.kit.QueryKit;
import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcDeleteOperation;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.r2dbc.core.ReactiveDeleteOperation;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.*;


/**
 * Implementation of {@link ReactiveDeleteOperation}.
 *
 * @author Mark Paluch
 * @since 1.1
 */
public final class R2dbcDeleteOperationSupport extends R2dbcOperationSupport implements R2dbcDeleteOperation {

    public R2dbcDeleteOperationSupport(ReactiveEntityTemplate template) {
        super(template);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.core.ReactiveDeleteOperation#deleteValue(java.lang.Class)
     */
    @NonNull
    @Override
    public <T> R2dbcDeleteOperation.R2dbcDelete<T> delete(@NonNull Class<T> domainType) {

        Assert.notNull(domainType, "DomainType must not be null");

        return new R2dbcDeleteSupport<>(template, domainType);
    }

    /**
     * 创建逻辑删除的Update对象
     *
     * @param entityClass entityClass
     */
    static <T> Update createLogicDeleteUpdate(Class<T> entityClass) {
        Pair<String, Object> logicDeleteColumn = MappingKit.getLogicDeleteColumn(entityClass, MappingKit.LogicDeleteValue.DELETE_VALUE);
        return Update.update(logicDeleteColumn.getFirst(), logicDeleteColumn.getSecond());
    }


    private final static class R2dbcDeleteSupport<T> extends R2dbcSupport<T> implements R2dbcDeleteOperation.R2dbcDelete<T> {

        R2dbcDeleteSupport(ReactiveEntityTemplate template, Class<T> domainType) {
            super(template, domainType);
        }

        R2dbcDeleteSupport(ReactiveEntityTemplate template, Class<T> domainType, Query query,
                           @Nullable SqlIdentifier tableName) {
            super(template, domainType, query, tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveDeleteOperation.DeleteWithTable#from(SqlIdentifier)
         */
        @NonNull
        @Override
        public ReactiveDeleteOperation.DeleteWithQuery from(@NonNull SqlIdentifier tableName) {

            Assert.notNull(tableName, "Table name must not be null");

            return new R2dbcDeleteSupport<>(this.template, this.domainType, this.query, tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveDeleteOperation.DeleteWithQuery#matching(org.springframework.data.r2dbc.query.Query)
         */
        @NonNull
        @Override
        public ReactiveDeleteOperation.TerminatingDelete matching(@NonNull Query query) {

            Assert.notNull(query, "Query must not be null");

            return new R2dbcDeleteSupport<>(this.template, this.domainType, query, this.tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveDeleteOperation.TerminatingDelete#all()
         */
        @NonNull
        @Override
        public Mono<Long> all() {
            return doDelete(this.query, this.domainType, this.tableName);
        }

        @Override
        public Mono<Boolean> using(T entity) {
            Assert.notNull(entity, "Entity must not be null");

            return doDelete(entity, this.tableName).map(it -> it > 0);
        }

        @Override
        public <ID> Mono<Boolean> byId(ID id) {
            Assert.notNull(id, "ID must not be empty");
            return byId(id, false);
        }

        @Override
        public <ID> Mono<Long> byIds(Collection<ID> ids) {
            return byIds(ids, false);
        }

        private <ID> Mono<Boolean> byId(ID id, boolean ignoreLogicDelete) {
            Assert.notNull(id, "ID must not be empty");
            Query query = QueryKit.queryById(this.domainType, id);
            return doDelete(query, this.domainType, this.tableName)
                    .map(it -> it > 0);
        }

        private <ID> Mono<Long> byIds(Collection<ID> ids, boolean ignoreLogicDelete) {
            Assert.notEmpty(ids, "ID must not be empty");
            Query query = QueryKit.queryById(this.domainType, ids);
            return doDelete(query, this.domainType, this.tableName, ignoreLogicDelete);
        }


        private <E> Mono<Long> doDelete(E entity, SqlIdentifier tableName) {
            return doDelete(entity, tableName, false);
        }

        @SuppressWarnings("SameParameterValue")
        private <E> Mono<Long> doDelete(E entity, SqlIdentifier tableName, boolean ignoreLogicDelete) {
            RelationalPersistentEntity<E> persistentEntity = MappingKit.getRequiredEntity(entity);
            return doDelete(getByIdQuery(entity, persistentEntity), persistentEntity.getType(), tableName, ignoreLogicDelete);
        }

        private <E> Mono<Long> doDelete(Query query, Class<E> entityClass, SqlIdentifier tableName) {
            return doDelete(query, entityClass, tableName, false);
        }

        private <E> Mono<Long> doDelete(Query query, Class<E> entityClass, SqlIdentifier tableName, boolean ignoreLogicDelete) {

            // 如果开启了逻辑删除，变为执行更新操作
            if (MappingKit.isLogicDeleteEnable(entityClass, ignoreLogicDelete)) {
                return new R2dbcUpdateOperationSupport(this.template)
                        .update(this.domainType)
                        .matching(query)
                        .apply(createLogicDeleteUpdate(entityClass));
            }

            StatementMapper statementMapper = this.statementMapper().forType(entityClass);

            StatementMapper.DeleteSpec deleteSpec = statementMapper.createDelete(tableName);

            Optional<CriteriaDefinition> criteria = query.getCriteria();
            if (criteria.isPresent()) {
                deleteSpec = criteria.map(deleteSpec::withCriteria).orElse(deleteSpec);
            }

            PreparedOperation<?> operation = statementMapper.getMappedObject(deleteSpec);
            return this.databaseClient().sql(operation).fetch().rowsUpdated().defaultIfEmpty(0L);
        }


        private <E> Query getByIdQuery(E entity, RelationalPersistentEntity<E> persistentEntity) {

            if (!persistentEntity.hasIdProperty()) {
                throw new MappingException("No id property found for object of type " + persistentEntity.getType());
            }

            IdentifierAccessor identifierAccessor = persistentEntity.getIdentifierAccessor(entity);
            Object id = identifierAccessor.getRequiredIdentifier();

            return Query.query(Criteria.where(persistentEntity.getRequiredIdProperty().getName()).is(id));
        }

    }

    /**
     * 提供给物理删除使用的适配器
     */
    record R2dbcDestroyAdapter(ReactiveEntityTemplate template) {

        <E, ID> Mono<Boolean> doDestroyById(ID id, Class<E> entityClass) {
            return new R2dbcDeleteSupport<>(template, entityClass).byId(id, true);
        }

        <E, ID> Mono<Long> doDestroyByIds(Collection<ID> ids, Class<E> entityClass) {
            return new R2dbcDeleteSupport<>(template, entityClass).byIds(ids, true);
        }


        <E> Mono<Long> doDestroy(E entity, SqlIdentifier tableName) {
            return new R2dbcDeleteSupport<>(template, entity.getClass()).doDelete(entity, tableName, true);
        }

        <E> Mono<Long> doDestroy(Query query, Class<E> entityClass, SqlIdentifier tableName) {
            return new R2dbcDeleteSupport<>(template, entityClass).doDelete(query, entityClass, tableName, true);
        }
    }


}
