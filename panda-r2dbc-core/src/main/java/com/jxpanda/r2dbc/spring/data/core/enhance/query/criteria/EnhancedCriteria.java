package com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.*;

/**
 * 直接复制了Criteria的源码
 * 在原来的基础上增加了Lambda的支持
 */
@SuppressWarnings({"unused", "BooleanMethodIsAlwaysInverted"})
public class EnhancedCriteria implements CriteriaDefinition {
    static final EnhancedCriteria EMPTY = new EnhancedCriteria(SqlIdentifier.EMPTY, Comparator.INITIAL, null);

    private final @Nullable EnhancedCriteria previous;
    private final Combinator combinator;
    private final List<CriteriaDefinition> group;
    private final @Nullable SqlIdentifier column;
    private final @Nullable Comparator comparator;
    private final @Nullable Object value;
    private final boolean ignoreCase;

    private EnhancedCriteria(SqlIdentifier column, Comparator comparator, @Nullable Object value) {
        this(null, Combinator.INITIAL, Collections.emptyList(), column, comparator, value, false);
    }

    private EnhancedCriteria(@Nullable EnhancedCriteria previous, Combinator combinator, List<CriteriaDefinition> group,
                             @Nullable SqlIdentifier column, @Nullable Comparator comparator, @Nullable Object value) {
        this(previous, combinator, group, column, comparator, value, false);
    }

    private EnhancedCriteria(@Nullable EnhancedCriteria previous, Combinator combinator, List<CriteriaDefinition> group,
                             @Nullable SqlIdentifier column, @Nullable Comparator comparator, @Nullable Object value, boolean ignoreCase) {

        this.previous = previous;
        this.combinator = previous != null && previous.isEmpty() ? Combinator.INITIAL : combinator;
        this.group = group;
        this.column = column;
        this.comparator = comparator;
        this.value = value;
        this.ignoreCase = ignoreCase;
    }

    private EnhancedCriteria(@Nullable EnhancedCriteria previous, Combinator combinator, List<CriteriaDefinition> group) {

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
     * @return an empty {@link EnhancedCriteria}.
     */
    public static EnhancedCriteria empty() {
        return EMPTY;
    }

    /**
     * Create a new {@link EnhancedCriteria} and combine it as group with {@code AND} using the provided {@link List LambdaCriterias}.
     *
     * @return new {@link EnhancedCriteria}.
     */
    public static EnhancedCriteria from(EnhancedCriteria... criteria) {

        Assert.notNull(criteria, "LambdaCriteria must not be null");
        Assert.noNullElements(criteria, "LambdaCriteria must not contain null elements");

        return from(Arrays.asList(criteria));
    }

    /**
     * Create a new {@link EnhancedCriteria} and combine it as group with {@code AND} using the provided {@link List LambdaCriterias}.
     *
     * @return new {@link EnhancedCriteria}.
     */
    public static EnhancedCriteria from(List<EnhancedCriteria> criteria) {

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
     * @return a new {@link EnhancedCriteriaStep} object to complete the first {@link EnhancedCriteria}.
     */
    public static <T> EnhancedCriteriaStep where(AccessorFunction<T, ?> accessorFunction) {

        Assert.notNull(accessorFunction, "Accessor function must not be null or empty");

        return where(accessorFunction.getColumnName());
    }


    /**
     * Static factory method to create a LambdaCriteria using the provided {@code column} name.
     *
     * @param column Must not be {@literal null} or empty.
     * @return a new {@link EnhancedCriteriaStep} object to complete the first {@link EnhancedCriteria}.
     */
    public static EnhancedCriteriaStep where(String column) {

        Assert.hasText(column, "Column name must not be null or empty");

        return new DefaultEnhancedCriteriaStep(SqlIdentifier.unquoted(column));
    }

    /**
     * Create a new {@link EnhancedCriteria} and combine it with {@code AND} using the provided {@code column} name.
     *
     * @param accessorFunction Must not be {@literal null} or empty.
     * @return a new {@link EnhancedCriteriaStep} object to complete the next {@link EnhancedCriteria}.
     */
    public <T> EnhancedCriteriaStep and(AccessorFunction<T, ?> accessorFunction) {

        Assert.notNull(accessorFunction, "Accessor function must not be null or empty");

        return and(accessorFunction.getColumnName());
    }


    /**
     * Create a new {@link EnhancedCriteria} and combine it with {@code AND} using the provided {@code column} name.
     *
     * @param column Must not be {@literal null} or empty.
     * @return a new {@link EnhancedCriteriaStep} object to complete the next {@link EnhancedCriteria}.
     */
    public EnhancedCriteriaStep and(String column) {

        Assert.hasText(column, "Column name must not be null or empty");

        SqlIdentifier identifier = SqlIdentifier.unquoted(column);
        return new DefaultEnhancedCriteriaStep(identifier) {
            @Override
            protected EnhancedCriteria createLambdaCriteria(Comparator comparator, @Nullable Object value) {
                return new EnhancedCriteria(EnhancedCriteria.this, Combinator.AND, Collections.emptyList(), identifier, comparator, value);
            }
        };
    }

    /**
     * Create a new {@link EnhancedCriteria} and combine it as group with {@code AND} using the provided {@link EnhancedCriteria} group.
     *
     * @param criteria criteria object.
     * @return a new {@link EnhancedCriteria} object.
     * @since 1.1
     */
    public EnhancedCriteria and(CriteriaDefinition criteria) {

        Assert.notNull(criteria, "LambdaCriteria must not be null");

        return and(Collections.singletonList(criteria));
    }

    /**
     * Create a new {@link EnhancedCriteria} and combine it as group with {@code AND} using the provided {@link EnhancedCriteria} group.
     *
     * @param criteria criteria objects.
     * @return a new {@link EnhancedCriteria} object.
     */
    @SuppressWarnings("unchecked")
    public EnhancedCriteria and(List<? extends CriteriaDefinition> criteria) {

        Assert.notNull(criteria, "LambdaCriteria must not be null");

        return new EnhancedCriteria(EnhancedCriteria.this, Combinator.AND, (List<CriteriaDefinition>) criteria);
    }

    /**
     * Create a new {@link EnhancedCriteria} and combine it with {@code OR} using the provided {@code column} name.
     *
     * @param column Must not be {@literal null} or empty.
     * @return a new {@link EnhancedCriteriaStep} object to complete the next {@link EnhancedCriteria}.
     */
    public EnhancedCriteriaStep or(String column) {

        Assert.hasText(column, "Column name must not be null or empty");

        SqlIdentifier identifier = SqlIdentifier.unquoted(column);
        return new DefaultEnhancedCriteriaStep(identifier) {
            @Override
            protected EnhancedCriteria createLambdaCriteria(Comparator comparator, @Nullable Object value) {
                return new EnhancedCriteria(EnhancedCriteria.this, Combinator.OR, Collections.emptyList(), identifier, comparator, value);
            }
        };
    }

    /**
     * Create a new {@link EnhancedCriteria} and combine it with {@code OR} using the provided {@code column} name.
     *
     * @param accessorFunction Must not be {@literal null} or empty.
     * @return a new {@link EnhancedCriteriaStep} object to complete the next {@link EnhancedCriteria}.
     */
    public <T> EnhancedCriteriaStep or(AccessorFunction<T, ?> accessorFunction) {

        Assert.notNull(accessorFunction, "Accessor function must not be null or empty");

        return or(accessorFunction.getColumnName());
    }

    /**
     * Create a new {@link EnhancedCriteria} and combine it as group with {@code OR} using the provided {@link EnhancedCriteria} group.
     *
     * @param criteria criteria object.
     * @return a new {@link EnhancedCriteria} object.
     * @since 1.1
     */
    public EnhancedCriteria or(CriteriaDefinition criteria) {

        Assert.notNull(criteria, "LambdaCriteria must not be null");

        return or(Collections.singletonList(criteria));
    }

    /**
     * Create a new {@link EnhancedCriteria} and combine it as group with {@code OR} using the provided {@link EnhancedCriteria} group.
     *
     * @param criteria criteria object.
     * @return a new {@link EnhancedCriteria} object.
     * @since 1.1
     */
    @SuppressWarnings("unchecked")
    public EnhancedCriteria or(List<? extends CriteriaDefinition> criteria) {

        Assert.notNull(criteria, "LambdaCriteria must not be null");

        return new EnhancedCriteria(EnhancedCriteria.this, Combinator.OR, (List<CriteriaDefinition>) criteria);
    }

    /**
     * Creates a new {@link EnhancedCriteria} with the given "ignore case" flag.
     *
     * @param ignoreCase {@literal true} if comparison should be done in case-insensitive way
     * @return a new {@link EnhancedCriteria} object
     */
    public EnhancedCriteria ignoreCase(boolean ignoreCase) {
        if (this.ignoreCase != ignoreCase) {
            return new EnhancedCriteria(previous, combinator, group, column, comparator, value, ignoreCase);
        }
        return this;
    }

    /**
     * @return the previous {@link EnhancedCriteria} object. Can be {@literal null} if there is no previous {@link EnhancedCriteria}.
     * @see #hasPrevious()
     */
    @Nullable
    public EnhancedCriteria getPrevious() {
        return previous;
    }

    /**
     * @return {@literal true} if this {@link EnhancedCriteria} has a previous one.
     */
    public boolean hasPrevious() {
        return previous != null;
    }


    /**
     * @return {@literal true} if this {@link EnhancedCriteria} is empty.
     * @since 1.1
     */
    @Override
    public boolean isEmpty() {

        if (!doIsEmpty()) {
            return false;
        }

        EnhancedCriteria parent = this.previous;

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
     * @return {@literal true} if this {@link EnhancedCriteria} is empty.
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

        if (value instanceof Number number) {
            return number.toString();
        }

        if (value instanceof Collection<?> collection) {

            StringJoiner joiner = new StringJoiner(", ");
            collection.forEach(o -> joiner.add(renderValue(o)));
            return joiner.toString();
        }

        if (value != null) {
            return String.format("'%s'", value);
        }

        return "null";
    }

    /**
     * Interface declaring terminal builder methods to build a {@link EnhancedCriteria}.
     */
    public interface EnhancedCriteriaStep {

        /**
         * Creates a {@link EnhancedCriteria} using equality.
         *
         * @param value must not be {@literal null}.
         */
        EnhancedCriteria is(Object value);

        /**
         * Creates a {@link EnhancedCriteria} using equality (is not).
         *
         * @param value must not be {@literal null}.
         */
        EnhancedCriteria not(Object value);

        /**
         * Creates a {@link EnhancedCriteria} using {@code IN}.
         *
         * @param values must not be {@literal null}.
         */
        EnhancedCriteria in(Object... values);

        /**
         * Creates a {@link EnhancedCriteria} using {@code IN}.
         *
         * @param values must not be {@literal null}.
         */
        EnhancedCriteria in(Collection<?> values);

        /**
         * Creates a {@link EnhancedCriteria} using {@code NOT IN}.
         *
         * @param values must not be {@literal null}.
         */
        EnhancedCriteria notIn(Object... values);

        /**
         * Creates a {@link EnhancedCriteria} using {@code NOT IN}.
         *
         * @param values must not be {@literal null}.
         */
        EnhancedCriteria notIn(Collection<?> values);

        /**
         * Creates a {@link EnhancedCriteria} using between ({@literal BETWEEN begin AND end}).
         *
         * @param begin must not be {@literal null}.
         * @param end   must not be {@literal null}.
         * @since 2.2
         */
        EnhancedCriteria between(Object begin, Object end);

        /**
         * Creates a {@link EnhancedCriteria} using not between ({@literal NOT BETWEEN begin AND end}).
         *
         * @param begin must not be {@literal null}.
         * @param end   must not be {@literal null}.
         * @since 2.2
         */
        EnhancedCriteria notBetween(Object begin, Object end);

        /**
         * Creates a {@link EnhancedCriteria} using less-than ({@literal <}).
         *
         * @param value must not be {@literal null}.
         */
        EnhancedCriteria lessThan(Object value);

        /**
         * Creates a {@link EnhancedCriteria} using less-than or equal to ({@literal <=}).
         *
         * @param value must not be {@literal null}.
         */
        EnhancedCriteria lessThanOrEquals(Object value);

        /**
         * Creates a {@link EnhancedCriteria} using greater-than({@literal >}).
         *
         * @param value must not be {@literal null}.
         */
        EnhancedCriteria greaterThan(Object value);

        /**
         * Creates a {@link EnhancedCriteria} using greater-than or equal to ({@literal >=}).
         *
         * @param value must not be {@literal null}.
         */
        EnhancedCriteria greaterThanOrEquals(Object value);

        /**
         * Creates a {@link EnhancedCriteria} using {@code LIKE}.
         *
         * @param value must not be {@literal null}.
         */
        EnhancedCriteria like(Object value);

        /**
         * Creates a {@link EnhancedCriteria} using {@code NOT LIKE}.
         *
         * @param value must not be {@literal null}
         * @return a new {@link EnhancedCriteria} object
         */
        EnhancedCriteria notLike(Object value);

        /**
         * Creates a {@link EnhancedCriteria} using {@code IS NULL}.
         */
        EnhancedCriteria isNull();

        /**
         * Creates a {@link EnhancedCriteria} using {@code IS NOT NULL}.
         */
        EnhancedCriteria isNotNull();

        /**
         * Creates a {@link EnhancedCriteria} using {@code IS TRUE}.
         *
         * @return a new {@link EnhancedCriteria} object
         */
        EnhancedCriteria isTrue();

        /**
         * Creates a {@link EnhancedCriteria} using {@code IS FALSE}.
         *
         * @return a new {@link EnhancedCriteria} object
         */
        EnhancedCriteria isFalse();
    }

    /**
     * Default {@link EnhancedCriteriaStep} implementation.
     */
    static class DefaultEnhancedCriteriaStep implements EnhancedCriteriaStep {

        private final SqlIdentifier property;

        DefaultEnhancedCriteriaStep(SqlIdentifier property) {
            this.property = property;
        }

        @Override
        public EnhancedCriteria is(Object value) {

            Assert.notNull(value, "Value must not be null");

            return createLambdaCriteria(Comparator.EQ, value);
        }

        @Override
        public EnhancedCriteria not(Object value) {

            Assert.notNull(value, "Value must not be null");

            return createLambdaCriteria(Comparator.NEQ, value);
        }

        @Override
        public EnhancedCriteria in(Object... values) {

            Assert.notNull(values, "Values must not be null");
            Assert.noNullElements(values, "Values must not contain a null value");

            if (values.length > 1 && values[1] instanceof Collection) {
                throw new InvalidDataAccessApiUsageException(
                        "You can only pass in one argument of type " + values[1].getClass().getName());
            }

            return createLambdaCriteria(Comparator.IN, Arrays.asList(values));
        }

        @Override
        public EnhancedCriteria in(Collection<?> values) {

            Assert.notNull(values, "Values must not be null");
            Assert.noNullElements(values.toArray(), "Values must not contain a null value");

            return createLambdaCriteria(Comparator.IN, values);
        }

        @Override
        public EnhancedCriteria notIn(Object... values) {

            Assert.notNull(values, "Values must not be null");
            Assert.noNullElements(values, "Values must not contain a null value");

            if (values.length > 1 && values[1] instanceof Collection) {
                throw new InvalidDataAccessApiUsageException(
                        "You can only pass in one argument of type " + values[1].getClass().getName());
            }

            return createLambdaCriteria(Comparator.NOT_IN, Arrays.asList(values));
        }

        @Override
        public EnhancedCriteria notIn(Collection<?> values) {

            Assert.notNull(values, "Values must not be null");
            Assert.noNullElements(values.toArray(), "Values must not contain a null value");

            return createLambdaCriteria(Comparator.NOT_IN, values);
        }

        @Override
        public EnhancedCriteria between(Object begin, Object end) {

            Assert.notNull(begin, "Begin value must not be null");
            Assert.notNull(end, "End value must not be null");

            return createLambdaCriteria(Comparator.BETWEEN, Pair.of(begin, end));
        }

        @Override
        public EnhancedCriteria notBetween(Object begin, Object end) {

            Assert.notNull(begin, "Begin value must not be null");
            Assert.notNull(end, "End value must not be null");

            return createLambdaCriteria(Comparator.NOT_BETWEEN, Pair.of(begin, end));
        }

        @Override
        public EnhancedCriteria lessThan(Object value) {

            Assert.notNull(value, "Value must not be null");

            return createLambdaCriteria(Comparator.LT, value);
        }

        @Override
        public EnhancedCriteria lessThanOrEquals(Object value) {

            Assert.notNull(value, "Value must not be null");

            return createLambdaCriteria(Comparator.LTE, value);
        }

        @Override
        public EnhancedCriteria greaterThan(Object value) {

            Assert.notNull(value, "Value must not be null");

            return createLambdaCriteria(Comparator.GT, value);
        }

        @Override
        public EnhancedCriteria greaterThanOrEquals(Object value) {

            Assert.notNull(value, "Value must not be null");

            return createLambdaCriteria(Comparator.GTE, value);
        }

        @Override
        public EnhancedCriteria like(Object value) {

            Assert.notNull(value, "Value must not be null");

            return createLambdaCriteria(Comparator.LIKE, value);
        }

        @Override
        public EnhancedCriteria notLike(Object value) {
            Assert.notNull(value, "Value must not be null");
            return createLambdaCriteria(Comparator.NOT_LIKE, value);
        }

        @Override
        public EnhancedCriteria isNull() {
            return createLambdaCriteria(Comparator.IS_NULL, null);
        }

        @Override
        public EnhancedCriteria isNotNull() {
            return createLambdaCriteria(Comparator.IS_NOT_NULL, null);
        }

        @Override
        public EnhancedCriteria isTrue() {
            return createLambdaCriteria(Comparator.IS_TRUE, true);
        }

        @Override
        public EnhancedCriteria isFalse() {
            return createLambdaCriteria(Comparator.IS_FALSE, false);
        }

        protected EnhancedCriteria createLambdaCriteria(Comparator comparator, @Nullable Object value) {
            return new EnhancedCriteria(this.property, comparator, value);
        }
    }
}
