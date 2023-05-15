package com.jxpanda.r2dbc.spring.data.core;

import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableJoin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.r2dbc.query.BoundAssignments;
import org.springframework.data.r2dbc.query.BoundCondition;
import org.springframework.data.r2dbc.query.UpdateMapper;
import org.springframework.data.relational.core.dialect.RenderContextFactory;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.sql.*;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.data.relational.core.sql.render.SqlRenderer;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.r2dbc.core.binding.BindMarkers;
import org.springframework.r2dbc.core.binding.BindTarget;
import org.springframework.r2dbc.core.binding.Bindings;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class R2dbcStatementMapper implements StatementMapper {

    private final R2dbcDialect dialect;
    private final RenderContext renderContext;
    private final UpdateMapper updateMapper;
    private final MappingContext<? extends RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> mappingContext;

    R2dbcStatementMapper(R2dbcDialect dialect, R2dbcConverter converter) {

        RenderContextFactory factory = new RenderContextFactory(dialect);

        this.dialect = dialect;
        this.renderContext = factory.createRenderContext();
        this.updateMapper = new UpdateMapper(dialect, converter);
        this.mappingContext = converter.getMappingContext();
    }


    @Override
    public <T> TypedStatementMapper<T> forType(Class<T> type) {

        Assert.notNull(type, "Type must not be null");

        return new DefaultTypedStatementMapper<>((RelationalPersistentEntity<T>) this.mappingContext.getRequiredPersistentEntity(type));
    }

    @Override
    public PreparedOperation<?> getMappedObject(SelectSpec selectSpec) {
        return getMappedObject(selectSpec, null);
    }

    private PreparedOperation<Select> getMappedObject(SelectSpec selectSpec,
                                                      @Nullable RelationalPersistentEntity<?> entity) {

        Table table = selectSpec.getTable();
        SelectBuilder.SelectAndFrom selectAndFrom = StatementBuilder.select(getSelectList(selectSpec, entity));

        if (selectSpec.isDistinct()) {
            selectAndFrom = selectAndFrom.distinct();
        }

        SelectBuilder.SelectFromAndJoin selectBuilder = selectAndFrom.from(table);

        BindMarkers bindMarkers = this.dialect.getBindMarkersFactory().create();
        Bindings bindings = Bindings.empty();
        CriteriaDefinition criteria = selectSpec.getCriteria();

        if (criteria != null && !criteria.isEmpty()) {

            BoundCondition mappedObject = this.updateMapper.getMappedObject(bindMarkers, criteria, table, entity);

            bindings = mappedObject.getBindings();
            selectBuilder.where(mappedObject.getCondition());
        }

        if (selectSpec.getSort().isSorted()) {
            List<OrderByField> sort = this.updateMapper.getMappedSort(table, selectSpec.getSort(), entity);
            selectBuilder.orderBy(sort);
        }

        if (selectSpec.getLimit() > 0) {
            selectBuilder.limit(selectSpec.getLimit());
        }

        if (selectSpec.getOffset() > 0) {
            selectBuilder.offset(selectSpec.getOffset());
        }

        if (selectSpec.getLock() != null) {
            selectBuilder.lock(selectSpec.getLock());
        }

        Select select;
        boolean isJoin = entity != null && entity.isAnnotationPresent(TableJoin.class);
        if (!isJoin) {
            select = selectBuilder.build();
        } else {
            TableJoin tableJoin = entity.getRequiredAnnotation(TableJoin.class);
            select = tableJoin.joinType().getFunction().apply(selectBuilder, Table.create(tableJoin.rightTable()))
                    .on(Conditions.just(tableJoin.on()))
                    .build();
        }

        return new DefaultPreparedOperation<>(select, this.renderContext, bindings);
    }

    protected List<Expression> getSelectList(SelectSpec selectSpec, @Nullable RelationalPersistentEntity<?> entity) {

        if (entity == null) {
            return selectSpec.getSelectList();
        }

        List<Expression> selectList = selectSpec.getSelectList();
        List<Expression> mapped = new ArrayList<>(selectList.size());

        for (Expression expression : selectList) {
            mapped.add(updateMapper.getMappedObject(expression, entity));
        }

        return mapped;
    }

    @Override
    public PreparedOperation<Insert> getMappedObject(InsertSpec insertSpec) {
        return getMappedObject(insertSpec, null);
    }

    private PreparedOperation<Insert> getMappedObject(InsertSpec insertSpec,
                                                      @Nullable RelationalPersistentEntity<?> entity) {

        BindMarkers bindMarkers = this.dialect.getBindMarkersFactory().create();
        Table table = Table.create(toSql(insertSpec.getTable()));

        BoundAssignments boundAssignments = this.updateMapper.getMappedObject(bindMarkers, insertSpec.getAssignments(),
                table, entity);

        Bindings bindings;

        bindings = boundAssignments.getBindings();

        InsertBuilder.InsertIntoColumnsAndValues insertBuilder = StatementBuilder.insert(table);
        InsertBuilder.InsertValuesWithBuild withBuild = (InsertBuilder.InsertValuesWithBuild) insertBuilder;

        for (Assignment assignment : boundAssignments.getAssignments()) {

            if (assignment instanceof AssignValue assignValue) {
                insertBuilder.column(assignValue.getColumn());
                withBuild = insertBuilder.value(assignValue.getValue());
            }
        }

        return new R2dbcStatementMapper.DefaultPreparedOperation<>(withBuild.build(), this.renderContext, bindings);
    }

    @Override
    public PreparedOperation<Update> getMappedObject(UpdateSpec updateSpec) {
        return getMappedObject(updateSpec, null);
    }


    private PreparedOperation<Update> getMappedObject(UpdateSpec updateSpec,
                                                      @Nullable RelationalPersistentEntity<?> entity) {

        BindMarkers bindMarkers = this.dialect.getBindMarkersFactory().create();
        Table table = Table.create(toSql(updateSpec.getTable()));

        if (updateSpec.getUpdate() == null || updateSpec.getUpdate().getAssignments().isEmpty()) {
            throw new IllegalArgumentException("UPDATE contains no assignments");
        }

        BoundAssignments boundAssignments = this.updateMapper.getMappedObject(bindMarkers,
                updateSpec.getUpdate().getAssignments(), table, entity);

        Bindings bindings;

        bindings = boundAssignments.getBindings();

        UpdateBuilder.UpdateWhere updateBuilder = StatementBuilder.update(table).set(boundAssignments.getAssignments());

        Update update;

        CriteriaDefinition criteria = updateSpec.getCriteria();
        if (criteria != null && !criteria.isEmpty()) {

            BoundCondition boundCondition = this.updateMapper.getMappedObject(bindMarkers, criteria, table, entity);

            bindings = bindings.and(boundCondition.getBindings());
            update = updateBuilder.where(boundCondition.getCondition()).build();
        } else {
            update = updateBuilder.build();
        }

        return new R2dbcStatementMapper.DefaultPreparedOperation<>(update, this.renderContext, bindings);
    }

    @Override
    public PreparedOperation<Delete> getMappedObject(DeleteSpec deleteSpec) {
        return getMappedObject(deleteSpec, null);
    }

    @Override
    public RenderContext getRenderContext() {
        return renderContext;
    }

    private PreparedOperation<Delete> getMappedObject(DeleteSpec deleteSpec,
                                                      @Nullable RelationalPersistentEntity<?> entity) {

        BindMarkers bindMarkers = this.dialect.getBindMarkersFactory().create();
        Table table = Table.create(toSql(deleteSpec.getTable()));

        DeleteBuilder.DeleteWhere deleteBuilder = StatementBuilder.delete(table);

        Bindings bindings = Bindings.empty();

        Delete delete;
        CriteriaDefinition criteria = deleteSpec.getCriteria();

        if (criteria != null && !criteria.isEmpty()) {

            BoundCondition boundCondition = this.updateMapper.getMappedObject(bindMarkers, deleteSpec.getCriteria(), table,
                    entity);

            bindings = boundCondition.getBindings();
            delete = deleteBuilder.where(boundCondition.getCondition()).build();
        } else {
            delete = deleteBuilder.build();
        }

        return new R2dbcStatementMapper.DefaultPreparedOperation<>(delete, this.renderContext, bindings);
    }

    private String toSql(SqlIdentifier identifier) {

        Assert.notNull(identifier, "SqlIdentifier must not be null");

        return identifier.toSql(this.dialect.getIdentifierProcessing());
    }

    /**
     * Default implementation of {@link PreparedOperation}.
     *
     * @param <T>
     */
    static class DefaultPreparedOperation<T> implements PreparedOperation<T> {

        private final T source;
        private final RenderContext renderContext;
        private final Bindings bindings;

        DefaultPreparedOperation(T source, RenderContext renderContext, Bindings bindings) {

            this.source = source;
            this.renderContext = renderContext;
            this.bindings = bindings;
        }

        @Override
        public T getSource() {
            return this.source;
        }

        @Override
        public String toQuery() {

            SqlRenderer sqlRenderer = SqlRenderer.create(this.renderContext);

            if (this.source instanceof Select select) {
                return sqlRenderer.render(select);
            }

            if (this.source instanceof Insert insert) {
                return sqlRenderer.render(insert);
            }

            if (this.source instanceof Update update) {
                return sqlRenderer.render(update);
            }

            if (this.source instanceof Delete delete) {
                return sqlRenderer.render(delete);
            }

            throw new IllegalStateException("Cannot render " + this.getSource());
        }

        @Override
        public void bindTo(BindTarget to) {
            this.bindings.apply(to);
        }

    }

    class DefaultTypedStatementMapper<T> implements TypedStatementMapper<T> {

        final RelationalPersistentEntity<T> entity;

        DefaultTypedStatementMapper(RelationalPersistentEntity<T> entity) {
            this.entity = entity;
        }

        @Override
        public <TC> TypedStatementMapper<TC> forType(Class<TC> type) {
            return R2dbcStatementMapper.this.forType(type);
        }

        @Override
        public PreparedOperation<?> getMappedObject(SelectSpec selectSpec) {
            return R2dbcStatementMapper.this.getMappedObject(selectSpec, this.entity);
        }

        @Override
        public PreparedOperation<?> getMappedObject(InsertSpec insertSpec) {
            return R2dbcStatementMapper.this.getMappedObject(insertSpec, this.entity);
        }

        @Override
        public PreparedOperation<?> getMappedObject(UpdateSpec updateSpec) {
            return R2dbcStatementMapper.this.getMappedObject(updateSpec, this.entity);
        }

        @Override
        public PreparedOperation<?> getMappedObject(DeleteSpec deleteSpec) {
            return R2dbcStatementMapper.this.getMappedObject(deleteSpec, this.entity);
        }

        @Override
        public RenderContext getRenderContext() {
            return R2dbcStatementMapper.this.getRenderContext();
        }
    }
}
