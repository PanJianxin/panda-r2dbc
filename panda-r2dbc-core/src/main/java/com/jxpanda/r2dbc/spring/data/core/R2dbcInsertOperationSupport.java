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

import com.jxpanda.r2dbc.spring.data.config.R2dbcEnvironment;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableId;
import com.jxpanda.r2dbc.spring.data.core.enhance.strategy.IdStrategy;
import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcInsertOperation;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.r2dbc.core.ReactiveInsertOperation;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;


/**
 * Implementation of {@link ReactiveInsertOperation}.
 *
 * @author Mark Paluch
 * @since 1.1
 */
public final class R2dbcInsertOperationSupport extends R2dbcOperationSupport implements R2dbcInsertOperation {

    public R2dbcInsertOperationSupport(ReactiveEntityTemplate template) {
        super(template);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.core.ReactiveInsertOperation#insert(java.lang.Class)
     */
    @NonNull
    @Override
    public <T> R2dbcInsert<T> insert(@NonNull Class<T> domainType) {

        Assert.notNull(domainType, "DomainType must not be null");

        return new R2dbcInsertSupport<>(this.template, domainType);
    }

    private final static class R2dbcInsertSupport<T> extends R2dbcSupport<T> implements R2dbcInsert<T> {

        R2dbcInsertSupport(ReactiveEntityTemplate template, Class<T> domainType) {
            super(template, domainType);
        }

        R2dbcInsertSupport(ReactiveEntityTemplate template, Class<T> domainType, @Nullable SqlIdentifier tableName) {
            super(template, domainType, null, tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveInsertOperation.InsertWithTable#into(SqlIdentifier)
         */
        @NonNull
        @Override
        public ReactiveInsertOperation.TerminatingInsert<T> into(@NonNull SqlIdentifier tableName) {

            Assert.notNull(tableName, "Table name must not be null");

            return new R2dbcInsertSupport<>(this.template, this.domainType, tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveInsertOperation.TerminatingInsert#one(java.lang.Object)
         */
        @NonNull
        @Override
        public Mono<T> using(@NonNull T object) {

            Assert.notNull(object, "Object to insert must not be null");

            return doInsert(object, this.tableName);
        }

        @Override
        public Flux<T> batch(Collection<T> objectList) {
            Assert.notEmpty(objectList, "Object list to insert must not be empty");
            return doInsertBatch(objectList, this.tableName);
        }

        private Mono<T> doInsert(T entity, SqlIdentifier tableName) {

            RelationalPersistentEntity<T> persistentEntity = R2dbcMappingKit.getRequiredEntity(entity);

            return template.maybeCallBeforeConvert(entity, tableName).flatMap(onBeforeConvert -> {

                T initializedEntity = setVersionIfNecessary(persistentEntity, onBeforeConvert);

                // id生成处理
                potentiallyGeneratorId(persistentEntity.getPropertyAccessor(entity), persistentEntity.getIdProperty());

                OutboundRow outboundRow = template.getDataAccessStrategy().getOutboundRow(initializedEntity);

                potentiallyRemoveId(persistentEntity, outboundRow);

                return template.maybeCallBeforeSave(initializedEntity, outboundRow, tableName)
                        .flatMap(entityToSave -> doInsert(entityToSave, tableName, outboundRow));
            });
        }

        private Mono<T> doInsert(T entity, SqlIdentifier tableName, OutboundRow outboundRow) {

            StatementMapper mapper = this.statementMapper();
            StatementMapper.InsertSpec insert = mapper.createInsert(tableName);

            for (SqlIdentifier column : outboundRow.keySet()) {
                @SuppressWarnings("deprecation")
                Parameter settableValue = outboundRow.get(column);
                if (settableValue.hasValue()) {
                    insert = insert.withColumn(column, settableValue);
                }
            }

            PreparedOperation<?> operation = mapper.getMappedObject(insert);

            List<SqlIdentifier> identifierColumns = getIdentifierColumns(entity.getClass());

            return this.databaseClient().sql(operation)
                    .filter(statement -> {

                        if (identifierColumns.isEmpty()) {
                            return statement.returnGeneratedValues();
                        }

                        return statement.returnGeneratedValues(this.template.getDataAccessStrategy().renderForGeneratedValues(identifierColumns.get(0)));
                    })
                    .map(this.converter().populateIdIfNecessary(entity)).all().last(entity)
                    .flatMap(saved -> this.template.maybeCallAfterSave(saved, outboundRow, tableName));
        }

        /**
         * 批量插入数据
         * 暂时使用循环来做
         * 后期考虑通过批量插入语句来做
         */
        private Flux<T> doInsertBatch(Collection<T> entityList, SqlIdentifier tableName) {
            // 这里要管理事务，这个函数不是public的，不能使用@Transactional注解来开启事务
            // 需要主动管理
            return Mono.just(entityList)
                    .filter(list -> !ObjectUtils.isEmpty(list))
                    .flatMapMany(Flux::fromIterable)
                    .flatMap(entity -> doInsert(entity, tableName))
                    .switchIfEmpty(Flux.empty())
                    .as(this.transactionalOperator()::transactional);
        }

        private List<SqlIdentifier> getIdentifierColumns(Class<?> clazz) {
            return template.getDataAccessStrategy().getIdentifierColumns(clazz);
        }

        private void potentiallyGeneratorId(PersistentPropertyAccessor<?> propertyAccessor, @Nullable RelationalPersistentProperty idProperty) {

            if (idProperty == null) {
                return;
            }

            if (shouldGeneratorIdValue(idProperty)) {
                Object generatedIdValue = idGenerator().generate();
                ConversionService conversionService = this.template.getConverter().getConversionService();
                propertyAccessor.setProperty(idProperty, conversionService.convert(generatedIdValue, idProperty.getType()));
            }
        }

        @SuppressWarnings("deprecation")
        private void potentiallyRemoveId(RelationalPersistentEntity<?> persistentEntity, OutboundRow outboundRow) {

            RelationalPersistentProperty idProperty = persistentEntity.getIdProperty();
            if (idProperty == null) {
                return;
            }

            SqlIdentifier columnName = idProperty.getColumnName();
            Parameter parameter = outboundRow.get(columnName);

            if (shouldSkipIdValue(parameter)) {
                outboundRow.remove(columnName);
            }
        }

        @SuppressWarnings("deprecation")
        private boolean shouldSkipIdValue(@Nullable Parameter value) {

            if (value == null || value.getValue() == null) {
                return true;
            }

            if (value.getValue() instanceof Number numberValue) {
                return numberValue.longValue() == 0L;
            }

            return false;
        }

        /**
         * 返回是否需要生成id
         * 基于IdStrategy的配置来判断
         *
         * @param idProperty idProperty
         */
        private boolean shouldGeneratorIdValue(RelationalPersistentProperty idProperty) {

            IdStrategy idStrategy = R2dbcEnvironment.getDatabase().idStrategy();

            TableId tableId = idProperty.findAnnotation(TableId.class);
            if (tableId != null) {
                idStrategy = tableId.idStrategy() == IdStrategy.DEFAULT ? idStrategy : tableId.idStrategy();
            }

            return idStrategy == IdStrategy.USE_GENERATOR;
        }


        @SuppressWarnings("unchecked")
        <E> E setVersionIfNecessary(RelationalPersistentEntity<E> persistentEntity, E entity) {

            RelationalPersistentProperty versionProperty = persistentEntity.getVersionProperty();
            if (versionProperty == null) {
                return entity;
            }

            Class<?> versionPropertyType = versionProperty.getType();
            Long version = versionPropertyType.isPrimitive() ? 1L : 0L;
            ConversionService conversionService = this.converter().getConversionService();
            PersistentPropertyAccessor<?> propertyAccessor = persistentEntity.getPropertyAccessor(entity);
            propertyAccessor.setProperty(versionProperty, conversionService.convert(version, versionPropertyType));

            return (E) propertyAccessor.getBean();
        }

    }
}
