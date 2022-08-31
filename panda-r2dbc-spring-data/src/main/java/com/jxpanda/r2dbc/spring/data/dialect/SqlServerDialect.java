package com.jxpanda.r2dbc.spring.data.dialect;

import org.springframework.r2dbc.core.binding.BindMarkersFactory;

import java.util.*;

/**
 * An SQL dialect for Microsoft SQL Server.
 *
 * @author Mark Paluch
 */
public class SqlServerDialect extends org.springframework.data.relational.core.dialect.SqlServerDialect
		implements R2dbcDialect {

	private static final Set<Class<?>> SIMPLE_TYPES = new HashSet<>(Collections.singletonList(UUID.class));

	/**
	 * Singleton instance.
	 */
	public static final SqlServerDialect INSTANCE = new SqlServerDialect();

	private static final BindMarkersFactory NAMED = BindMarkersFactory.named("@", "P", 32,
			SqlServerDialect::filterBindMarker);

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.r2dbc.dialect.Dialect#getBindMarkersFactory()
	 */
	@Override
	public BindMarkersFactory getBindMarkersFactory() {
		return NAMED;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.r2dbc.dialect.Dialect#getSimpleTypesKeys()
	 */
	@Override
	public Collection<? extends Class<?>> getSimpleTypes() {
		return SIMPLE_TYPES;
	}

	private static String filterBindMarker(CharSequence input) {

		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < input.length(); i++) {

			char ch = input.charAt(i);

			// ascii letter or digit
			if (Character.isLetterOrDigit(ch) && ch < 127) {
				builder.append(ch);
			}
		}

		if (builder.length() == 0) {
			return "";
		}

		return "_" + builder.toString();
	}
}
