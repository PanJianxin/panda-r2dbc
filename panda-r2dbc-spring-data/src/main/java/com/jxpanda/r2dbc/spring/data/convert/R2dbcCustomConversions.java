package com.jxpanda.r2dbc.spring.data.convert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.jxpanda.r2dbc.spring.data.mapping.R2dbcSimpleTypeHolder;
import com.jxpanda.r2dbc.spring.data.mapping.handler.TypeHandler;
import org.springframework.data.convert.CustomConversions;
import com.jxpanda.r2dbc.spring.data.dialect.R2dbcDialect;

/**
 * Value object to capture custom conversion. {@link R2dbcCustomConversions} also act as factory for
 * {@link org.springframework.data.mapping.model.SimpleTypeHolder}
 *
 * @author Mark Paluch
 * @see CustomConversions
 * @see org.springframework.data.mapping.model.SimpleTypeHolder
 */
public class R2dbcCustomConversions extends CustomConversions {

    public static final List<Object> STORE_CONVERTERS;

    public static final StoreConversions STORE_CONVERSIONS;


    static {

        List<Object> converters = new ArrayList<>(R2dbcConverters.getConvertersToRegister());

        STORE_CONVERTERS = Collections.unmodifiableList(converters);
        STORE_CONVERSIONS = StoreConversions.of(R2dbcSimpleTypeHolder.HOLDER, STORE_CONVERTERS);
    }

    /**
     * Create a new {@link R2dbcCustomConversions} instance registering the given converters.
     *
     * @param converters must not be {@literal null}.
     * @deprecated since 1.3, use {@link #of(R2dbcDialect, Object...)} or
     * {@link #R2dbcCustomConversions(StoreConversions, Collection)} directly to consider dialect-native
     * simple types. Use {@link CustomConversions.StoreConversions#NONE} to omit store-specific converters.
     */
    @Deprecated
    public R2dbcCustomConversions(Collection<?> converters) {
        super(new R2dbcCustomConversionsConfiguration(STORE_CONVERSIONS,
                converters instanceof List ? (List<?>) converters : new ArrayList<>(converters)));
    }

    /**
     * Create a new {@link R2dbcCustomConversions} instance registering the given converters.
     *
     * @param storeConversions must not be {@literal null}.
     * @param converters       must not be {@literal null}.
     */
    public R2dbcCustomConversions(StoreConversions storeConversions, Collection<?> converters) {
        super(new R2dbcCustomConversionsConfiguration(storeConversions,
                converters instanceof List ? (List<?>) converters : new ArrayList<>(converters)));
    }

    protected R2dbcCustomConversions(ConverterConfiguration converterConfiguration) {
        super(converterConfiguration);
    }

    /**
     * Create a new {@link R2dbcCustomConversions} from the given {@link R2dbcDialect} and {@code converters}.
     *
     * @param dialect    must not be {@literal null}.
     * @param converters must not be {@literal null}.
     * @return the custom conversions object.
     * @since 1.2
     */
    public static R2dbcCustomConversions of(R2dbcDialect dialect, Object... converters) {
        return of(dialect, Arrays.asList(converters));
    }

    /**
     * Create a new {@link R2dbcCustomConversions} from the given {@link R2dbcDialect} and {@code converters}.
     *
     * @param dialect    must not be {@literal null}.
     * @param converters must not be {@literal null}.
     * @return the custom conversions object.
     * @since 1.2
     */
    public static R2dbcCustomConversions of(R2dbcDialect dialect, Collection<?> converters) {

        List<Object> storeConverters = new ArrayList<>(dialect.getConverters());
        storeConverters.addAll(R2dbcCustomConversions.STORE_CONVERTERS);

        return new R2dbcCustomConversions(StoreConversions.of(dialect.getSimpleTypeHolder(), storeConverters), converters);
    }

    static class R2dbcCustomConversionsConfiguration extends ConverterConfiguration {

        public R2dbcCustomConversionsConfiguration(StoreConversions storeConversions, List<?> userConverters) {
            super(storeConversions, userConverters, convertiblePair -> {

                // Avoid JSR-310 temporal types conversion into java.util.Date
                if (convertiblePair.getSourceType().getName().startsWith("java.time.")
                        && convertiblePair.getTargetType().equals(Date.class)) {
                    return false;
                }

                return true;
            });
        }
    }
}
