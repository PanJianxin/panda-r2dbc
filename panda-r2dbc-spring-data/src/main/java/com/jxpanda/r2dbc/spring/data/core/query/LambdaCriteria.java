package com.jxpanda.r2dbc.spring.data.core.query;

import com.jxpanda.r2dbc.spring.data.extension.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.extension.annotation.TableId;
import com.jxpanda.r2dbc.spring.data.extension.constant.StringConstant;
import com.jxpanda.r2dbc.spring.data.extension.support.AccessorFunction;
import com.jxpanda.r2dbc.spring.data.extension.support.ReflectionKit;
import com.jxpanda.r2dbc.spring.data.extension.support.StringKit;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 直接复制了Criteria的源码
 * 添加了lambda的支持，其他都没有变
 */
@SuppressWarnings({"unused", "BooleanMethodIsAlwaysInverted"})
public class LambdaCriteria implements CriteriaDefinition {
    static final LambdaCriteria EMPTY = new LambdaCriteria(SqlIdentifier.EMPTY, Comparator.INITIAL, null);

    private final @Nullable LambdaCriteria previous;
    private final Combinator combinator;
    private final List<CriteriaDefinition> group;

    private final @Nullable SqlIdentifier column;
    private final @Nullable Comparator comparator;
    private final @Nullable Object value;
    private final boolean ignoreCase;

    private LambdaCriteria(SqlIdentifier column, Comparator comparator, @Nullable Object value) {
        this(null, Combinator.INITIAL, Collections.emptyList(), column, comparator, value, false);
    }

    private LambdaCriteria(@Nullable LambdaCriteria previous, Combinator combinator, List<CriteriaDefinition> group,
                           @Nullable SqlIdentifier column, @Nullable Comparator comparator, @Nullable Object value) {
        this(previous, combinator, group, column, comparator, value, false);
    }

    private LambdaCriteria(@Nullable LambdaCriteria previous, Combinator combinator, List<CriteriaDefinition> group,
                           @Nullable SqlIdentifier column, @Nullable Comparator comparator, @Nullable Object value, boolean ignoreCase) {

        this.previous = previous;
        this.combinator = previous != null && previous.isEmpty() ? Combinator.INITIAL : combinator;
        this.group = group;
        this.column = column;
        this.comparator = comparator;
        this.value = value;
        this.ignoreCase = ignoreCase;
    }

    private LambdaCriteria(@Nullable LambdaCriteria previous, Combinator combinator, List<CriteriaDefinition> group) {

        this.previous = previous;
        this.combinator = previous != null && previous.isEmpty() ? Combinator.INITIAL : combinator;
        this.group = group;
        this.column = null;
        this.comparator = null;
        this.value = null;
        this.ignoreCase = false;
    }

    /**
     * Static factory method to create an empty LambdaCriteria.
     *
     * @return an empty {@link LambdaCriteria}.
     */
    public static LambdaCriteria empty() {
        return EMPTY;
    }

    /**
     * Create a new {@link LambdaCriteria} and combine it as group with {@code AND} using the provided {@link List LambdaCriterias}.
     *
     * @return new {@link LambdaCriteria}.
     */
    public static LambdaCriteria from(LambdaCriteria... criteria) {

        Assert.notNull(criteria, "LambdaCriteria must not be null");
        Assert.noNullElements(criteria, "LambdaCriteria must not contain null elements");

        return from(Arrays.asList(criteria));
    }

    /**
     * Create a new {@link LambdaCriteria} and combine it as group with {@code AND} using the provided {@link List LambdaCriterias}.
     *
     * @return new {@link LambdaCriteria}.
     */
    public static LambdaCriteria from(List<LambdaCriteria> criteria) {

        Assert.notNull(criteria, "LambdaCriteria must not be null");
        Assert.noNullElements(criteria, "LambdaCriteria must not contain null elements");

        if (criteria.isEmpty()) {
            return EMPTY;
        }

        if (criteria.size() == 1) {
            return criteria.get(0);
        }

        return EMPTY.and(criteria);
    }

    /**
     * Static factory method to create a LambdaCriteria using the provided {@code column} name.
     *
     * @param accessorFunction Must not be {@literal null} or empty.
     * @return a new {@link LambdaCriteria.LambdaCriteriaStep} object to complete the first {@link LambdaCriteria}.
     */
    public static <T> LambdaCriteria.LambdaCriteriaStep where(AccessorFunction<T, ?> accessorFunction) {

        Assert.notNull(accessorFunction, "Accessor function must not be null or empty");

        return where(accessorFunction.getColumnName());
    }


    /**
     * Static factory method to create a LambdaCriteria using the provided {@code column} name.
     *
     * @param column Must not be {@literal null} or empty.
     * @return a new {@link LambdaCriteria.LambdaCriteriaStep} object to complete the first {@link LambdaCriteria}.
     */
    public static LambdaCriteria.LambdaCriteriaStep where(String column) {

        Assert.hasText(column, "Column name must not be null or empty");

        return new LambdaCriteria.DefaultLambdaCriteriaStep(SqlIdentifier.unquoted(column));
    }

    /**
     * Create a new {@link LambdaCriteria} and combine it with {@code AND} using the provided {@code column} name.
     *
     * @param accessorFunction Must not be {@literal null} or empty.
     * @return a new {@link LambdaCriteria.LambdaCriteriaStep} object to complete the next {@link LambdaCriteria}.
     */
    public <T> LambdaCriteria.LambdaCriteriaStep and(AccessorFunction<T, ?> accessorFunction) {

        Assert.notNull(accessorFunction, "Accessor function must not be null or empty");

        return and(accessorFunction.getColumnName());
    }


    /**
     * Create a new {@link LambdaCriteria} and combine it with {@code AND} using the provided {@code column} name.
     *
     * @param column Must not be {@literal null} or empty.
     * @return a new {@link LambdaCriteria.LambdaCriteriaStep} object to complete the next {@link LambdaCriteria}.
     */
    public LambdaCriteria.LambdaCriteriaStep and(String column) {

        Assert.hasText(column, "Column name must not be null or empty");

        SqlIdentifier identifier = SqlIdentifier.unquoted(column);
        return new LambdaCriteria.DefaultLambdaCriteriaStep(identifier) {
            @Override
            protected LambdaCriteria createLambdaCriteria(Comparator comparator, @Nullable Object value) {
                return new LambdaCriteria(LambdaCriteria.this, Combinator.AND, Collections.emptyList(), identifier, comparator, value);
            }
        };
    }

    /**
     * Create a new {@link LambdaCriteria} and combine it as group with {@code AND} using the provided {@link LambdaCriteria} group.
     *
     * @param criteria criteria object.
     * @return a new {@link LambdaCriteria} object.
     * @since 1.1
     */
    public LambdaCriteria and(CriteriaDefinition criteria) {

        Assert.notNull(criteria, "LambdaCriteria must not be null");

        return and(Collections.singletonList(criteria));
    }

    /**
     * Create a new {@link LambdaCriteria} and combine it as group with {@code AND} using the provided {@link LambdaCriteria} group.
     *
     * @param criteria criteria objects.
     * @return a new {@link LambdaCriteria} object.
     */
    @SuppressWarnings("unchecked")
    public LambdaCriteria and(List<? extends CriteriaDefinition> criteria) {

        Assert.notNull(criteria, "LambdaCriteria must not be null");

        return new LambdaCriteria(LambdaCriteria.this, Combinator.AND, (List<CriteriaDefinition>) criteria);
    }

    /**
     * Create a new {@link LambdaCriteria} and combine it with {@code OR} using the provided {@code column} name.
     *
     * @param column Must not be {@literal null} or empty.
     * @return a new {@link LambdaCriteria.LambdaCriteriaStep} object to complete the next {@link LambdaCriteria}.
     */
    public LambdaCriteria.LambdaCriteriaStep or(String column) {

        Assert.hasText(column, "Column name must not be null or empty");

        SqlIdentifier identifier = SqlIdentifier.unquoted(column);
        return new LambdaCriteria.DefaultLambdaCriteriaStep(identifier) {
            @Override
            protected LambdaCriteria createLambdaCriteria(Comparator comparator, @Nullable Object value) {
                return new LambdaCriteria(LambdaCriteria.this, Combinator.OR, Collections.emptyList(), identifier, comparator, value);
            }
        };
    }

    /**
     * Create a new {@link LambdaCriteria} and combine it with {@code OR} using the provided {@code column} name.
     *
     * @param accessorFunction Must not be {@literal null} or empty.
     * @return a new {@link LambdaCriteria.LambdaCriteriaStep} object to complete the next {@link LambdaCriteria}.
     */
    public <T> LambdaCriteria.LambdaCriteriaStep or(AccessorFunction<T, ?> accessorFunction) {

        Assert.notNull(accessorFunction, "Accessor function must not be null or empty");

        return or(accessorFunction.getColumnName());
    }

    /**
     * Create a new {@link LambdaCriteria} and combine it as group with {@code OR} using the provided {@link LambdaCriteria} group.
     *
     * @param criteria criteria object.
     * @return a new {@link LambdaCriteria} object.
     * @since 1.1
     */
    public LambdaCriteria or(CriteriaDefinition criteria) {

        Assert.notNull(criteria, "LambdaCriteria must not be null");

        return or(Collections.singletonList(criteria));
    }

    /**
     * Create a new {@link LambdaCriteria} and combine it as group with {@code OR} using the provided {@link LambdaCriteria} group.
     *
     * @param criteria criteria object.
     * @return a new {@link LambdaCriteria} object.
     * @since 1.1
     */
    @SuppressWarnings("unchecked")
    public LambdaCriteria or(List<? extends CriteriaDefinition> criteria) {

        Assert.notNull(criteria, "LambdaCriteria must not be null");

        return new LambdaCriteria(LambdaCriteria.this, Combinator.OR, (List<CriteriaDefinition>) criteria);
    }

    /**
     * Creates a new {@link LambdaCriteria} with the given "ignore case" flag.
     *
     * @param ignoreCase {@literal true} if comparison should be done in case-insensitive way
     * @return a new {@link LambdaCriteria} object
     */
    public LambdaCriteria ignoreCase(boolean ignoreCase) {
        if (this.ignoreCase != ignoreCase) {
            return new LambdaCriteria(previous, combinator, group, column, comparator, value, ignoreCase);
        }
        return this;
    }

    /**
     * @return the previous {@link LambdaCriteria} object. Can be {@literal null} if there is no previous {@link LambdaCriteria}.
     * @see #hasPrevious()
     */
    @Nullable
    public LambdaCriteria getPrevious() {
        return previous;
    }

    /**
     * @return {@literal true} if this {@link LambdaCriteria} has a previous one.
     */
    public boolean hasPrevious() {
        return previous != null;
    }

//    private static String getColumnNameFromLambda(AccessorFunction<?, ?> accessorFunction) {
//        Field field = ReflectionKit.getField(accessorFunction);
//
//        String columnName = StringConstant.BLANK;
//
//        TableId tableId = field.getAnnotation(TableId.class);
//        if (tableId != null) {
//            columnName = tableId.name();
//        }
//
//        if (StringKit.isBlank(columnName)) {
//            TableColumn tableColumn = field.getAnnotation(TableColumn.class);
//            if (tableColumn != null) {
//                columnName = tableColumn.name();
//            }
//        }
//
//        return StringKit.isBlank(columnName) ? field.getName() : columnName;
//    }

    /**
     * @return {@literal true} if this {@link LambdaCriteria} is empty.
     * @since 1.1
     */
    @Override
    public boolean isEmpty() {

        if (!doIsEmpty()) {
            return false;
        }

        LambdaCriteria parent = this.previous;

        while (parent != null) {

            if (!parent.doIsEmpty()) {
                return false;
            }

            parent = parent.previous;
        }

        return true;
    }

    private boolean doIsEmpty() {

        if (this.comparator == Comparator.INITIAL) {
            return true;
        }

        if (this.column != null) {
            return false;
        }

        for (CriteriaDefinition criteria : group) {

            if (!criteria.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return {@literal true} if this {@link LambdaCriteria} is empty.
     */
    public boolean isGroup() {
        return !this.group.isEmpty();
    }

    /**
     * @return {@link Combinator} to combine this criteria with a previous one.
     */
    @NonNull
    public Combinator getCombinator() {
        return combinator;
    }

    @NonNull
    @Override
    public List<CriteriaDefinition> getGroup() {
        return group;
    }

    /**
     * @return the column/property name.
     */
    @Nullable
    public SqlIdentifier getColumn() {
        return column;
    }

    /**
     * @return {@link Comparator}.
     */
    @Nullable
    public Comparator getComparator() {
        return comparator;
    }

    /**
     * @return the comparison value. Can be {@literal null}.
     */
    @Nullable
    public Object getValue() {
        return value;
    }

    /**
     * Checks whether comparison should be done in case-insensitive way.
     *
     * @return {@literal true} if comparison should be done in case-insensitive way
     */
    @Override
    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    @Override
    public String toString() {

        if (isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        unroll(this, builder);

        return builder.toString();
    }

    private void unroll(CriteriaDefinition criteria, StringBuilder stringBuilder) {

        CriteriaDefinition current = criteria;

        // reverse unroll criteria chain
        Map<CriteriaDefinition, CriteriaDefinition> forwardChain = new HashMap<>();

        while (Objects.requireNonNull(current).hasPrevious()) {
            forwardChain.put(current.getPrevious(), current);
            current = current.getPrevious();
        }

        // perform the actual mapping
        render(current, stringBuilder);
        while (forwardChain.containsKey(current)) {

            CriteriaDefinition criterion = forwardChain.get(current);

            if (criterion.getCombinator() != Combinator.INITIAL) {
                stringBuilder.append(' ').append(criterion.getCombinator().name()).append(' ');
            }

            render(criterion, stringBuilder);

            current = criterion;
        }
    }

    private void unrollGroup(List<? extends CriteriaDefinition> criteria, StringBuilder stringBuilder) {

        stringBuilder.append("(");

        boolean first = true;
        for (CriteriaDefinition criterion : criteria) {

            if (criterion.isEmpty()) {
                continue;
            }

            if (!first) {
                Combinator combinator = criterion.getCombinator() == Combinator.INITIAL ? Combinator.AND
                        : criterion.getCombinator();
                stringBuilder.append(' ').append(combinator.name()).append(' ');
            }

            unroll(criterion, stringBuilder);
            first = false;
        }

        stringBuilder.append(")");
    }

    private void render(CriteriaDefinition criteria, StringBuilder stringBuilder) {

        if (criteria.isEmpty()) {
            return;
        }

        if (criteria.isGroup()) {
            unrollGroup(criteria.getGroup(), stringBuilder);
            return;
        }

        stringBuilder.append(Objects.requireNonNull(criteria.getColumn()).toSql(IdentifierProcessing.NONE)).append(' ')
                .append(Objects.requireNonNull(criteria.getComparator()).getComparator());

        switch (criteria.getComparator()) {
            case BETWEEN, NOT_BETWEEN -> {
                if (criteria.getValue() instanceof Pair<?, ?> pair) {
                    stringBuilder.append(' ').append(pair.getFirst()).append(" AND ").append(pair.getSecond());
                }
            }
            case IS_NULL, IS_NOT_NULL, IS_TRUE, IS_FALSE -> {
            }
            case IN, NOT_IN -> {
                stringBuilder.append(" (").append(renderValue(criteria.getValue())).append(')');
            }
            default -> stringBuilder.append(' ').append(renderValue(criteria.getValue()));
        }
    }

    private static String renderValue(@Nullable Object value) {

        if (value instanceof Number) {
            return value.toString();
        }

        if (value instanceof Collection) {

            StringJoiner joiner = new StringJoiner(", ");
            ((Collection<?>) value).forEach(o -> joiner.add(renderValue(o)));
            return joiner.toString();
        }

        if (value != null) {
            return String.format("'%s'", value);
        }

        return "null";
    }

    /**
     * Interface declaring terminal builder methods to build a {@link LambdaCriteria}.
     */
    public interface LambdaCriteriaStep {

        /**
         * Creates a {@link LambdaCriteria} using equality.
         *
         * @param value must not be {@literal null}.
         */
        LambdaCriteria is(Object value);

        /**
         * Creates a {@link LambdaCriteria} using equality (is not).
         *
         * @param value must not be {@literal null}.
         */
        LambdaCriteria not(Object value);

        /**
         * Creates a {@link LambdaCriteria} using {@code IN}.
         *
         * @param values must not be {@literal null}.
         */
        LambdaCriteria in(Object... values);

        /**
         * Creates a {@link LambdaCriteria} using {@code IN}.
         *
         * @param values must not be {@literal null}.
         */
        LambdaCriteria in(Collection<?> values);

        /**
         * Creates a {@link LambdaCriteria} using {@code NOT IN}.
         *
         * @param values must not be {@literal null}.
         */
        LambdaCriteria notIn(Object... values);

        /**
         * Creates a {@link LambdaCriteria} using {@code NOT IN}.
         *
         * @param values must not be {@literal null}.
         */
        LambdaCriteria notIn(Collection<?> values);

        /**
         * Creates a {@link LambdaCriteria} using between ({@literal BETWEEN begin AND end}).
         *
         * @param begin must not be {@literal null}.
         * @param end   must not be {@literal null}.
         * @since 2.2
         */
        LambdaCriteria between(Object begin, Object end);

        /**
         * Creates a {@link LambdaCriteria} using not between ({@literal NOT BETWEEN begin AND end}).
         *
         * @param begin must not be {@literal null}.
         * @param end   must not be {@literal null}.
         * @since 2.2
         */
        LambdaCriteria notBetween(Object begin, Object end);

        /**
         * Creates a {@link LambdaCriteria} using less-than ({@literal <}).
         *
         * @param value must not be {@literal null}.
         */
        LambdaCriteria lessThan(Object value);

        /**
         * Creates a {@link LambdaCriteria} using less-than or equal to ({@literal <=}).
         *
         * @param value must not be {@literal null}.
         */
        LambdaCriteria lessThanOrEquals(Object value);

        /**
         * Creates a {@link LambdaCriteria} using greater-than({@literal >}).
         *
         * @param value must not be {@literal null}.
         */
        LambdaCriteria greaterThan(Object value);

        /**
         * Creates a {@link LambdaCriteria} using greater-than or equal to ({@literal >=}).
         *
         * @param value must not be {@literal null}.
         */
        LambdaCriteria greaterThanOrEquals(Object value);

        /**
         * Creates a {@link LambdaCriteria} using {@code LIKE}.
         *
         * @param value must not be {@literal null}.
         */
        LambdaCriteria like(Object value);

        /**
         * Creates a {@link LambdaCriteria} using {@code NOT LIKE}.
         *
         * @param value must not be {@literal null}
         * @return a new {@link LambdaCriteria} object
         */
        LambdaCriteria notLike(Object value);

        /**
         * Creates a {@link LambdaCriteria} using {@code IS NULL}.
         */
        LambdaCriteria isNull();

        /**
         * Creates a {@link LambdaCriteria} using {@code IS NOT NULL}.
         */
        LambdaCriteria isNotNull();

        /**
         * Creates a {@link LambdaCriteria} using {@code IS TRUE}.
         *
         * @return a new {@link LambdaCriteria} object
         */
        LambdaCriteria isTrue();

        /**
         * Creates a {@link LambdaCriteria} using {@code IS FALSE}.
         *
         * @return a new {@link LambdaCriteria} object
         */
        LambdaCriteria isFalse();
    }

    /**
     * Default {@link LambdaCriteria.LambdaCriteriaStep} implementation.
     */
    static class DefaultLambdaCriteriaStep implements LambdaCriteria.LambdaCriteriaStep {

        private final SqlIdentifier property;

        DefaultLambdaCriteriaStep(SqlIdentifier property) {
            this.property = property;
        }

        @Override
        public LambdaCriteria is(Object value) {

            Assert.notNull(value, "Value must not be null");

            return createLambdaCriteria(Comparator.EQ, value);
        }

        @Override
        public LambdaCriteria not(Object value) {

            Assert.notNull(value, "Value must not be null");

            return createLambdaCriteria(Comparator.NEQ, value);
        }

        @Override
        public LambdaCriteria in(Object... values) {

            Assert.notNull(values, "Values must not be null");
            Assert.noNullElements(values, "Values must not contain a null value");

            if (values.length > 1 && values[1] instanceof Collection) {
                throw new InvalidDataAccessApiUsageException(
                        "You can only pass in one argument of type " + values[1].getClass().getName());
            }

            return createLambdaCriteria(Comparator.IN, Arrays.asList(values));
        }

        @Override
        public LambdaCriteria in(Collection<?> values) {

            Assert.notNull(values, "Values must not be null");
            Assert.noNullElements(values.toArray(), "Values must not contain a null value");

            return createLambdaCriteria(Comparator.IN, values);
        }

        @Override
        public LambdaCriteria notIn(Object... values) {

            Assert.notNull(values, "Values must not be null");
            Assert.noNullElements(values, "Values must not contain a null value");

            if (values.length > 1 && values[1] instanceof Collection) {
                throw new InvalidDataAccessApiUsageException(
                        "You can only pass in one argument of type " + values[1].getClass().getName());
            }

            return createLambdaCriteria(Comparator.NOT_IN, Arrays.asList(values));
        }

        @Override
        public LambdaCriteria notIn(Collection<?> values) {

            Assert.notNull(values, "Values must not be null");
            Assert.noNullElements(values.toArray(), "Values must not contain a null value");

            return createLambdaCriteria(Comparator.NOT_IN, values);
        }

        @Override
        public LambdaCriteria between(Object begin, Object end) {

            Assert.notNull(begin, "Begin value must not be null");
            Assert.notNull(end, "End value must not be null");

            return createLambdaCriteria(Comparator.BETWEEN, Pair.of(begin, end));
        }

        @Override
        public LambdaCriteria notBetween(Object begin, Object end) {

            Assert.notNull(begin, "Begin value must not be null");
            Assert.notNull(end, "End value must not be null");

            return createLambdaCriteria(Comparator.NOT_BETWEEN, Pair.of(begin, end));
        }

        @Override
        public LambdaCriteria lessThan(Object value) {

            Assert.notNull(value, "Value must not be null");

            return createLambdaCriteria(Comparator.LT, value);
        }

        @Override
        public LambdaCriteria lessThanOrEquals(Object value) {

            Assert.notNull(value, "Value must not be null");

            return createLambdaCriteria(Comparator.LTE, value);
        }

        @Override
        public LambdaCriteria greaterThan(Object value) {

            Assert.notNull(value, "Value must not be null");

            return createLambdaCriteria(Comparator.GT, value);
        }

        @Override
        public LambdaCriteria greaterThanOrEquals(Object value) {

            Assert.notNull(value, "Value must not be null");

            return createLambdaCriteria(Comparator.GTE, value);
        }

        @Override
        public LambdaCriteria like(Object value) {

            Assert.notNull(value, "Value must not be null");

            return createLambdaCriteria(Comparator.LIKE, value);
        }

        @Override
        public LambdaCriteria notLike(Object value) {
            Assert.notNull(value, "Value must not be null");
            return createLambdaCriteria(Comparator.NOT_LIKE, value);
        }

        @Override
        public LambdaCriteria isNull() {
            return createLambdaCriteria(Comparator.IS_NULL, null);
        }

        @Override
        public LambdaCriteria isNotNull() {
            return createLambdaCriteria(Comparator.IS_NOT_NULL, null);
        }

        @Override
        public LambdaCriteria isTrue() {
            return createLambdaCriteria(Comparator.IS_TRUE, true);
        }

        @Override
        public LambdaCriteria isFalse() {
            return createLambdaCriteria(Comparator.IS_FALSE, false);
        }

        protected LambdaCriteria createLambdaCriteria(Comparator comparator, @Nullable Object value) {
            return new LambdaCriteria(this.property, comparator, value);
        }
    }
}
