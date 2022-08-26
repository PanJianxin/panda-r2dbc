package com.jxpanda.r2dbc.spring.data.dialect;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.jxpanda.r2dbc.spring.data.mapping.R2dbcSimpleTypeHolder;
import com.jxpanda.r2dbc.spring.data.mapping.handler.TypeHandler;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.r2dbc.core.binding.BindMarkersFactory;

/**
 * R2DBC-specific extension to {@link Dialect}. Represents a dialect that is implemented by a particular database.
 *
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Michael Berry
 */
public interface R2dbcDialect extends Dialect {

    /**
     * Returns the {@link BindMarkersFactory} used by this dialect.
     *
     * @return the {@link BindMarkersFactory} used by this dialect.
     */
    BindMarkersFactory getBindMarkersFactory();

    /**
     * Return a collection of types that are natively supported by this database/driver. Defaults to
     * {@link Collections#emptySet()}.
     *
     * @return a collection of types that are natively supported by this database/driver. Defaults to
     * {@link Collections#emptySet()}.
     */
    default Collection<? extends Class<?>> getSimpleTypes() {
        return Collections.emptySet();
    }

    /**
     * Return the {@link SimpleTypeHolder} for this dialect.
     *
     * @return the {@link SimpleTypeHolder} for this dialect.
     * @see #getSimpleTypes()
     */
    default SimpleTypeHolder getSimpleTypeHolder() {

        Set<Class<?>> simpleTypes = new HashSet<>(getSimpleTypes());
        simpleTypes.addAll(R2dbcSimpleTypeHolder.R2DBC_SIMPLE_TYPES);

        return new SimpleTypeHolder(simpleTypes, true);
    }

    /**
     * Return a collection of converters for this dialect.
     *
     * @return a collection of converters for this dialect.
     */
    default Collection<Object> getConverters() {
        return Collections.emptySet();
    }

    /**
     * Render a {@link SqlIdentifier} in a way suitable for registering it as a generated key with a statement through
     * {@code Statement#returnGeneratedValues}. The default implementation renders it as it would render a SQL
     * representation of the identifier, i.e. with quotes where applicable.
     *
     * @param identifier to render. Must not be {@literal null}.
     * @return rendered identifier. Guaranteed to be not {@literal null}.
     * @since 1.3.2
     */
    default String renderForGeneratedValues(SqlIdentifier identifier) {
        return identifier.toSql(getIdentifierProcessing());
    }

    /**
     * Return a collection of handlers for this dialect.
     * 返回一个类型处理器，理论上可以通过扩展getSimpleTypes()来实现功能
     * 但是为了不过多的侵入原有的设计，选择独立扩展一个接口
     */
    default Collection<TypeHandler> getTypeHandlers() {
        return Collections.emptySet();
    }

}
