package com.jxpanda.r2dbc.spring.data.core.expander;

import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.Parameter;

@FunctionalInterface
public interface NamedParameterProvider {

    /**
     * Returns the {@link Parameter value} for a parameter identified either by name or by index.
     *
     * @param index parameter index according the parameter discovery order.
     * @param name  name of the parameter.
     * @return the bindable value. Returning a {@literal null} value raises
     * {@link org.springframework.dao.InvalidDataAccessApiUsageException} in named parameter processing.
     */
    @Nullable
    Parameter getParameter(int index, String name);
}