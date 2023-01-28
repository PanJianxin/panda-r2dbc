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
import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcUpdateOperation;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link R2dbcUpdateOperation}.
 *
 * @author Mark Paluch
 * @since 1.1
 */
public final class R2dbcUpdateOperationSupport extends R2dbcOperationSupport implements R2dbcUpdateOperation {


    public R2dbcUpdateOperationSupport(ReactiveEntityTemplate template) {
        super(template);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.core.ReactiveUpdateOperation#update(java.lang.Class)
     */
    @NonNull
    @Override
    public R2dbcUpdate update(@NonNull Class<?> domainType) {

        Assert.notNull(domainType, "DomainType must not be null");

        return new R2dbcUpdateSupport<>(this.template, domainType);
    }


    @SuppressWarnings("unchecked")
    private final static class R2dbcUpdateSupport<T> extends R2dbcSupport<T> implements R2dbcUpdate, TerminatingUpdate {

        R2dbcUpdateSupport(ReactiveEntityTemplate template, Class<T> domainType) {
            super(template, domainType);
        }

        R2dbcUpdateSupport(ReactiveEntityTemplate template, Class<T> domainType, Query query,
                           @Nullable SqlIdentifier tableName) {
            super(template, domainType, query, tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveUpdateOperation.UpdateWithTable#inTable(SqlIdentifier)
         */
        @NonNull
        @Override
        public UpdateWithQuery inTable(@NonNull SqlIdentifier tableName) {

            Assert.notNull(tableName, "Table name must not be null");

            return new R2dbcUpdateSupport<>(this.template, this.domainType, this.query, tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveUpdateOperation.UpdateWithQuery#matching(org.springframework.data.r2dbc.query.Query)
         */
        @NonNull
        @Override
        public TerminatingUpdate matching(@NonNull Query query) {

            Assert.notNull(query, "Query must not be null");

            return new R2dbcUpdateSupport<>(this.template, this.domainType, query, this.tableName);
        }

        @NonNull
        @Override
        public Mono<Long> apply(@NonNull Update update) {
            Assert.notNull(update, "Update must not be null");
            return doUpdate(this.query, update, this.domainType, this.tableName);
        }

        @Override
        public <E> Mono<E> using(E entity) {
            return doUpdate(entity, this.tableName);
        }


        @SuppressWarnings("deprecation")
        private <E> Mono<E> doUpdate(E entity, SqlIdentifier tableName) {


            RelationalPersistentEntity<E> persistentEntity = MappingKit.getRequiredEntity(entity);

            return template.maybeCallBeforeConvert(entity, tableName).flatMap(onBeforeConvert -> {

                E entityToUse;
                Criteria matchingVersionCriteria;

                if (persistentEntity.hasVersionProperty()) {
                    matchingVersionCriteria = createMatchingVersionCriteria(onBeforeConvert, persistentEntity);
                    entityToUse = incrementVersion(persistentEntity, onBeforeConvert);
                } else {
                    entityToUse = onBeforeConvert;
                    matchingVersionCriteria = null;
                }

                OutboundRow outboundRow = getOutboundRow(entityToUse);

                return template.maybeCallBeforeSave(entityToUse, outboundRow, tableName).flatMap(onBeforeSave -> {

                    SqlIdentifier idColumn = persistentEntity.getRequiredIdProperty().getColumnName();
                    Parameter id = outboundRow.remove(idColumn);

                    persistentEntity.forEach(property -> {
                        if (property.isInsertOnly() || !MappingKit.isPropertyEffective(entityToUse, persistentEntity, property)) {
                            outboundRow.remove(property.getColumnName());
                        }
                    });

                    Criteria criteria = Criteria.where(template.getDataAccessStrategy().toSql(idColumn)).is(id);

                    if (matchingVersionCriteria != null) {
                        criteria = criteria.and(matchingVersionCriteria);
                    }

                    return doUpdate(onBeforeSave, tableName, persistentEntity, criteria, outboundRow);
                });
            });
        }

        private <E> Mono<Long> doUpdate(Query query, Update update, Class<E> entityClass, SqlIdentifier tableName) {

            StatementMapper statementMapper = this.statementMapper().forType(entityClass);

            StatementMapper.UpdateSpec selectSpec = statementMapper.createUpdate(tableName, update);

            Optional<CriteriaDefinition> criteria = query.getCriteria();
            if (criteria.isPresent()) {
                selectSpec = criteria.map(selectSpec::withCriteria).orElse(selectSpec);
            }

            PreparedOperation<?> operation = statementMapper.getMappedObject(selectSpec);
            return this.databaseClient().sql(operation).fetch().rowsUpdated();
        }

        @SuppressWarnings("rawtypes")
        private <E> Mono<E> doUpdate(E entity, SqlIdentifier tableName, RelationalPersistentEntity<E> persistentEntity, Criteria criteria, OutboundRow outboundRow) {


            Update update = Update.from((Map) outboundRow);

            StatementMapper mapper = this.statementMapper();
            StatementMapper.UpdateSpec updateSpec = mapper.createUpdate(tableName, update).withCriteria(criteria);

            PreparedOperation<?> operation = mapper.getMappedObject(updateSpec);

            return this.databaseClient().sql(operation).fetch().rowsUpdated().handle((rowsUpdated, sink) -> {

                if (rowsUpdated != 0) {
                    return;
                }

                if (persistentEntity.hasVersionProperty()) {
                    sink.error(new OptimisticLockingFailureException(formatOptimisticLockingExceptionMessage(entity, persistentEntity)));
                } else {
                    sink.error(new TransientDataAccessResourceException(formatTransientEntityExceptionMessage(entity, persistentEntity)));
                }
            }).then(template.maybeCallAfterSave(entity, outboundRow, tableName));
        }


        private <E> Criteria createMatchingVersionCriteria(E entity, RelationalPersistentEntity<E> persistentEntity) {

            PersistentPropertyAccessor<?> propertyAccessor = persistentEntity.getPropertyAccessor(entity);

            Optional<RelationalPersistentProperty> versionPropertyOptional = Optional.ofNullable(persistentEntity.getVersionProperty());

            return versionPropertyOptional.map(versionProperty -> {
                Object version = propertyAccessor.getProperty(versionProperty);
                Criteria.CriteriaStep versionColumn = Criteria.where(template.getDataAccessStrategy().toSql(versionProperty.getColumnName()));
                if (version == null) {
                    return versionColumn.isNull();
                } else {
                    return versionColumn.is(version);
                }
            }).orElse(Criteria.empty());

        }

        private <E> E incrementVersion(RelationalPersistentEntity<E> persistentEntity, E entity) {

            PersistentPropertyAccessor<?> propertyAccessor = persistentEntity.getPropertyAccessor(entity);

            Optional<RelationalPersistentProperty> versionPropertyOptional = Optional.ofNullable(persistentEntity.getVersionProperty());

            versionPropertyOptional.ifPresent(versionProperty -> {
                ConversionService conversionService = this.converter().getConversionService();
                Optional<Object> currentVersionValue = Optional.ofNullable(propertyAccessor.getProperty(versionProperty));

                long newVersionValue = currentVersionValue.map(it -> conversionService.convert(it, Long.class)).map(it -> it + 1).orElse(1L);

                propertyAccessor.setProperty(versionProperty, conversionService.convert(newVersionValue, versionProperty.getType()));
            });
            return (E) propertyAccessor.getBean();
        }

        private <E> String formatOptimisticLockingExceptionMessage(E entity, RelationalPersistentEntity<E> persistentEntity) {

            return String.format("Failed to update table [%s]; Version does not match for row with Id [%s]", persistentEntity.getQualifiedTableName(), persistentEntity.getIdentifierAccessor(entity).getIdentifier());
        }

        private <E> String formatTransientEntityExceptionMessage(E entity, RelationalPersistentEntity<E> persistentEntity) {

            return String.format("Failed to update table [%s]; Row with Id [%s] does not exist", persistentEntity.getQualifiedTableName(), persistentEntity.getIdentifierAccessor(entity).getIdentifier());
        }

    }
}
