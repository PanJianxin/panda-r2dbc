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
package com.jxpanda.r2dbc.spring.data.repository.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.r2dbc.core.ReactiveDataAccessStrategy;
import com.jxpanda.r2dbc.spring.data.core.StatementMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.relational.repository.Lock;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.sql.*;
import org.springframework.data.relational.repository.query.RelationalEntityMetadata;
import org.springframework.data.relational.repository.query.RelationalParameterAccessor;
import org.springframework.data.relational.repository.query.RelationalQueryCreator;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.PreparedOperation;

/**
 * Implementation of {@link AbstractQueryCreator} that creates {@link PreparedOperation} from a {@link PartTree}.
 *
 * @author Roman Chigvintsev
 * @author Mark Paluch
 * @author Mingyuan Wu
 * @author Myeonghyeon Lee
 * @author Diego Krupitza
 * @since 1.1
 */
class R2dbcQueryCreator extends RelationalQueryCreator<PreparedOperation<?>> {

	private final PartTree tree;
	private final RelationalParameterAccessor accessor;
	private final ReactiveDataAccessStrategy dataAccessStrategy;
	private final RelationalEntityMetadata<?> entityMetadata;
	private final List<String> projectedProperties;
	private final Class<?> entityToRead;
	private final Optional<Lock> lock;

	/**
	 * Creates new instance of this class with the given {@link PartTree}, {@link ReactiveDataAccessStrategy},
	 * {@link RelationalEntityMetadata} and {@link RelationalParameterAccessor}.
	 *
	 * @param tree part tree, must not be {@literal null}.
	 * @param dataAccessStrategy data access strategy, must not be {@literal null}.
	 * @param entityMetadata relational entity metadata, must not be {@literal null}.
	 * @param accessor parameter metadata provider, must not be {@literal null}.
	 * @param projectedProperties properties to project, must not be {@literal null}.
	 */
	public R2dbcQueryCreator(PartTree tree, ReactiveDataAccessStrategy dataAccessStrategy,
			RelationalEntityMetadata<?> entityMetadata, RelationalParameterAccessor accessor,
			List<String> projectedProperties, Optional<Lock> lock) {
		super(tree, accessor);

		this.tree = tree;
		this.accessor = accessor;

		this.dataAccessStrategy = dataAccessStrategy;
		this.entityMetadata = entityMetadata;
		this.projectedProperties = projectedProperties;
		this.entityToRead = entityMetadata.getTableEntity().getType();
		this.lock = lock;
	}

	/**
	 * Creates {@link PreparedOperation} applying the given {@link Criteria} and {@link Sort} definition.
	 *
	 * @param criteria {@link Criteria} to be applied to query
	 * @param sort sort option to be applied to query, must not be {@literal null}.
	 * @return instance of {@link PreparedOperation}
	 */
	@Override
	protected PreparedOperation<?> complete(@Nullable Criteria criteria, Sort sort) {

		StatementMapper statementMapper = dataAccessStrategy.getStatementMapper().forType(entityToRead);

		if (tree.isDelete()) {
			return delete(criteria, statementMapper);
		}

		return select(criteria, sort, statementMapper);
	}

	private PreparedOperation<?> delete(@Nullable Criteria criteria, StatementMapper statementMapper) {

		StatementMapper.DeleteSpec deleteSpec = statementMapper.createDelete(entityMetadata.getTableName())
				.withCriteria(criteria);

		return statementMapper.getMappedObject(deleteSpec);
	}

	private PreparedOperation<?> select(@Nullable Criteria criteria, Sort sort, StatementMapper statementMapper) {

		StatementMapper.SelectSpec selectSpec = statementMapper.createSelect(entityMetadata.getTableName())
				.withProjection(getSelectProjection());

		if (tree.isExistsProjection()) {
			selectSpec = selectSpec.limit(1);
		} else if (tree.isLimiting()) {
			selectSpec = selectSpec.limit(tree.getMaxResults());
		}

		Pageable pageable = accessor.getPageable();
		if (pageable.isPaged()) {
			selectSpec = selectSpec.limit(pageable.getPageSize()).offset(pageable.getOffset());
		}

		if (criteria != null) {
			selectSpec = selectSpec.withCriteria(criteria);
		}

		if (sort.isSorted()) {
			selectSpec = selectSpec.withSort(getSort(sort));
		}

		if (tree.isDistinct()) {
			selectSpec = selectSpec.distinct();
		}

		if (this.lock.isPresent()) {
			selectSpec = selectSpec.lock(this.lock.get().value());
		}

		return statementMapper.getMappedObject(selectSpec);
	}

	private Expression[] getSelectProjection() {

		List<Expression> expressions;

		Table table = Table.create(entityMetadata.getTableName());
		if (!projectedProperties.isEmpty()) {

			RelationalPersistentEntity<?> entity = entityMetadata.getTableEntity();
			expressions = new ArrayList<>(projectedProperties.size());

			for (String projectedProperty : projectedProperties) {

				RelationalPersistentProperty property = entity.getPersistentProperty(projectedProperty);
				Column column = table.column(property != null //
						? property.getColumnName() //
						: SqlIdentifier.unquoted(projectedProperty));
				expressions.add(column);
			}

		} else if (tree.isExistsProjection()) {

			expressions = dataAccessStrategy.getIdentifierColumns(entityToRead).stream() //
					.map(table::column) //
					.collect(Collectors.toList());
		} else if (tree.isCountProjection()) {

			Expression countExpression = entityMetadata.getTableEntity().hasIdProperty()
					? table.column(entityMetadata.getTableEntity().getRequiredIdProperty().getColumnName())
					: Expressions.asterisk();

			expressions = Collections.singletonList(Functions.count(countExpression));
		} else {
			expressions = dataAccessStrategy.getAllColumns(entityToRead).stream() //
					.map(table::column) //
					.collect(Collectors.toList());
		}

		return expressions.toArray(new Expression[0]);
	}

	private Sort getSort(Sort sort) {

		RelationalPersistentEntity<?> tableEntity = entityMetadata.getTableEntity();

		List<Sort.Order> orders = sort.get().map(order -> {
			RelationalPersistentProperty property = tableEntity.getRequiredPersistentProperty(order.getProperty());
			return order.isAscending() ? Sort.Order.asc(property.getName()) : Sort.Order.desc(property.getName());
		}).collect(Collectors.toList());

		return Sort.by(orders);
	}
}
