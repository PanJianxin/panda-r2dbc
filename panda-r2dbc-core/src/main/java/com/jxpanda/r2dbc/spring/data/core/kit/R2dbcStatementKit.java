package com.jxpanda.r2dbc.spring.data.core.kit;

import com.jxpanda.r2dbc.spring.data.config.R2dbcEnvironment;
import com.jxpanda.r2dbc.spring.data.config.properties.LogicDeletePluginProperties;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableJoin;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria.EnhancedCriteria;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.CollectionKit;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.StringKit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.r2dbc.query.BoundCondition;
import org.springframework.data.r2dbc.query.UpdateMapper;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.sql.*;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.binding.BindMarkers;
import org.springframework.r2dbc.core.binding.Bindings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class R2dbcStatementKit {

    public static <E> boolean isJoin(@Nullable Class<E> entityClass) {
        return entityClass != null && isJoin(R2dbcMappingKit.getPersistentEntity(entityClass));
    }

    public static <E> boolean isJoin(@Nullable RelationalPersistentEntity<E> relationalPersistentEntity) {
        return relationalPersistentEntity != null && relationalPersistentEntity.isAnnotationPresent(TableJoin.class);
    }

    public static <E> SelectHandler buildHandler(Table table,
                                                 SelectBuilder.SelectFromAndJoin selectBuilder,
                                                 RelationalPersistentEntity<E> relationalPersistentEntity,
                                                 CriteriaDefinition criteria,
                                                 BindMarkers bindMarkers,
                                                 UpdateMapper updateMapper) {
        boolean isJoin = isJoin(relationalPersistentEntity);
        Table rightTable = null;
        TableJoin tableJoin = null;
        if (isJoin) {
            tableJoin = relationalPersistentEntity.getRequiredAnnotation(TableJoin.class);
            rightTable = Table.create(tableJoin.rightTable());
        }
        List<RelationalPersistentProperty> referenceProperties = R2dbcMappingKit.getProperties(relationalPersistentEntity);
        Map<String, RelationalPersistentProperty> propertyMap = referenceProperties.stream()
                .filter(it -> it.isAnnotationPresent(TableColumn.class))
                .flatMap(it -> {
                    TableColumn tableColumn = it.getRequiredAnnotation(TableColumn.class);
                    String name = tableColumn.name();
                    String alias = tableColumn.alias();
                    Map<String, RelationalPersistentProperty> columnMap = new HashMap<>(2);
                    columnMap.put(name, it);
                    if (StringKit.isNotBlank(alias)) {
                        columnMap.put(alias, it);
                    }
                    return columnMap.entrySet().stream();
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));
        return SelectHandler.builder()
                .isJoin(isJoin(relationalPersistentEntity))
                .entity(relationalPersistentEntity)
                .propertyMap(propertyMap)
                .selectBuilder(selectBuilder)
                .tableJoin(tableJoin)
                .leftTable(table)
                .rightTable(rightTable)
                .criteria(criteria)
                .bindMarkers(bindMarkers)
                .bindings(Bindings.empty())
                .updateMapper(updateMapper)
                .build();
    }


    @Getter
    @Builder
    @AllArgsConstructor
    public static class SelectHandler {
        private boolean isJoin;
        private SelectBuilder.SelectFromAndJoin selectBuilder;
        private CriteriaDefinition criteria;
        private BindMarkers bindMarkers;
        private Bindings bindings;
        private Table leftTable;
        private Table rightTable;
        private TableJoin tableJoin;
        private RelationalPersistentEntity<?> entity;
        private Map<String, RelationalPersistentProperty> propertyMap;
        private UpdateMapper updateMapper;

        public Select buildSelect() {
            Select select;
            if (!isJoin) {
                select = selectBuilder.build();
            } else {
                select = tableJoin.joinType().getFunction()
                        .apply(selectBuilder, rightTable)
                        .on(Conditions.just(tableJoin.on()))
                        .build();
            }
            return select;
        }

        public void handleWhere() {
            if (!isJoin) {
                BoundCondition mappedObject = this.updateMapper.getMappedObject(bindMarkers, criteria, leftTable, entity);
                bindings = mappedObject.getBindings();
                selectBuilder.where(mappedObject.getCondition());

            } else if (criteria != null) {
                SelectBuilder.SelectWhereAndOr where = null;
                for (CriteriaDefinition current = criteria; current != null; current = current.getPrevious()) {
                    SqlIdentifier column = current.getColumn();
                    if (column != null) {
                        where = addToWhere(where, column, current);
                    } else if (CollectionKit.isNotEmpty(current.getGroup())) {
                        for (CriteriaDefinition group : current.getGroup()) {
                            where = addToWhere(where, group.getColumn(), group);
                        }
                    }
                }
            }
        }

        private SelectBuilder.SelectWhereAndOr addToWhere(@Nullable SelectBuilder.SelectWhereAndOr where, @Nullable SqlIdentifier column, CriteriaDefinition criteria) {
            if (column == null || column.isEmpty()) {
                return where;
            }
            BoundCondition boundCondition = handleColumn(column, criteria);
            if (boundCondition == null) {
                return where;
            }
            this.bindings = this.bindings.and(boundCondition.getBindings());
            if (where == null) {
                return selectBuilder.where(boundCondition.getCondition());
            }
            return where.and(boundCondition.getCondition());
        }

        private BoundCondition handleColumn(SqlIdentifier column, CriteriaDefinition criteria) {
            if (column != null && !column.isEmpty()) {
                Table table;
                String columnName = column.getReference();
                RelationalPersistentProperty persistentProperty = propertyMap.get(columnName);
                LogicDeletePluginProperties logicDeleteProperties = R2dbcEnvironment.getLogicDeleteProperties();
                CriteriaDefinition thisCriteria = criteria;
                if (logicDeleteProperties.enable() && logicDeleteProperties.field().equals(columnName) || persistentProperty == null) {
                    table = leftTable;
                } else {
                    TableColumn tableColumn = persistentProperty.getRequiredAnnotation(TableColumn.class);
                    String fromTable = tableColumn.fromTable();
                    table = StringKit.isBlank(fromTable) ? leftTable : Table.create(fromTable);
                    if (columnName.equals(tableColumn.alias())) {
                        thisCriteria = EnhancedCriteria.replaceColumn(criteria, SqlIdentifier.unquoted(tableColumn.name()));
                    }
                }
                return this.updateMapper.getMappedObject(bindMarkers, thisCriteria, table, entity);
            }
            return null;
        }
    }


}
