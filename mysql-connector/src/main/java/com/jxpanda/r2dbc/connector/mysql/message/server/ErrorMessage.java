/*
 * Copyright 2018-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jxpanda.r2dbc.connector.mysql.message.server;

import io.netty.buffer.ByteBuf;
import io.r2dbc.spi.*;
import reactor.util.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.jxpanda.r2dbc.connector.mysql.util.AssertUtils.requireNonNull;

/**
 * A message considers an error that's reported by server-side. Not like JDBC MySQL, the SQL state is an
 * independent property.
 * <p>
 * The {@link #offendingSql} will be bound by statement flow, protocol layer will always get {@code null}.
 */
public final class ErrorMessage implements ServerMessage {

    private static final String CONSTRAINT_VIOLATION_PREFIX = "23";

    private static final String TRANSACTION_ROLLBACK_PREFIX = "40";

    private static final String SYNTAX_ERROR_PREFIX = "42";

    private static final int SQL_STATE_SIZE = 5;

    private final int code;

    @Nullable
    private final String sqlState;

    private final String message;

    @Nullable
    private final String offendingSql;

    private ErrorMessage(int code, @Nullable String sqlState, String message) {
        this(code, sqlState, message, null);
    }

    private ErrorMessage(int code, @Nullable String sqlState, String message, @Nullable String offendingSql) {
        this.code = code;
        this.sqlState = sqlState;
        this.message = requireNonNull(message, "message must not be null");
        this.offendingSql = offendingSql;
    }

    public R2dbcException toException() {
        return toException(offendingSql);
    }

    public R2dbcException toException(@Nullable String sql) {
        // Should keep looking more error codes
        switch (code) { // Database access denied
            // Wrong password
            // Kill thread denied
            // Table access denied
            // Column access denied
            // Operation has no privilege(s)
            // Routine or process access denied
            // User need password but has no password
            case 1044, 1045, 1095, 1142, 1143, 1227, 1370, 1698, 1873 -> { // Change user denied
                return new R2dbcPermissionDeniedException(message, sqlState, code);
            } // Read interrupted, reading packet timeout because of network jitter in most cases
            // Write interrupted, writing packet timeout because of network jitter in most cases
            // Dead-lock :-( no one wants this
            case 1159, 1161, 1213, 1317 -> { // Statement execution interrupted
                return new R2dbcTransientResourceException(message, sqlState, code);
            } // Wait lock timeout
            case 1205, 1907 -> { // Statement executing timeout
                return new R2dbcTimeoutException(message, sqlState, code);
            }
            case 1613 -> { // Transaction rollback because of took too long
                return new R2dbcRollbackException(message, sqlState, code);
            } // Table already exists
            // Unknown table
            // Unknown column name in existing table
            // Bad syntax
            // Unsupported reference
            // Unknown table name
            // Something already exists, like savepoint
            // Something does not exists, like savepoint
            case 1050, 1051, 1054, 1064, 1247, 1146, 1304, 1305, 1630 -> { // Function not exists
                return new R2dbcBadGrammarException(message, sqlState, code, sql);
            } // Duplicate key
            // Field cannot be null
            // Duplicate entry for key constraint
            // Violation of an unique constraint
            // Add a foreign key has a violation
            // Child row has a violation of foreign key constraint when inserting or updating
            // Parent row has a violation of foreign key constraint when deleting or updating
            // Field has no default value but user try set it to DEFAULT
            // Parent row has a violation of foreign key constraint when deleting or updating
            // Child row has a violation of foreign key constraint when inserting or updating
            // Conflicting foreign key constraints and unique constraints
            case 1022, 1048, 1062, 1169, 1215, 1216, 1217, 1364, 1451, 1452, 1557, 1859 -> { // Duplicate unknown entry for key constraint
                return new R2dbcDataIntegrityViolationException(message, sqlState, code);
            }
        }

        if (sqlState == null) {
            // Has no SQL state, all exceptions mismatch, fallback.
            return new R2dbcNonTransientResourceException(message, null, code);
        } else if (sqlState.startsWith(SYNTAX_ERROR_PREFIX)) {
            return new R2dbcBadGrammarException(message, sqlState, code, sql);
        } else if (sqlState.startsWith(CONSTRAINT_VIOLATION_PREFIX)) {
            return new R2dbcDataIntegrityViolationException(message, sqlState, code);
        } else if (sqlState.startsWith(TRANSACTION_ROLLBACK_PREFIX)) {
            return new R2dbcRollbackException(message, sqlState, code);
        }

        // Uncertain SQL state, all exceptions mismatch, fallback.
        return new R2dbcNonTransientResourceException(message, null, code);
    }

    public int getCode() {
        return code;
    }

    @Nullable
    public String getSqlState() {
        return sqlState;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Creates a new {@link ErrorMessage} with specific offending statement.
     *
     * @param sql offending statement.
     * @return {@code this} if {@code sql} is {@code null}, otherwise a new {@link ErrorMessage}.
     */
    public ErrorMessage offendedBy(@Nullable String sql) {
        return sql == null ? this : new ErrorMessage(code, sqlState, message, sql);
    }

    /**
     * Decode error message from a {@link ByteBuf}.
     *
     * @param buf the {@link ByteBuf}.
     * @return decoded error message.
     */
    public static ErrorMessage decode(ByteBuf buf) {
        buf.skipBytes(1); // 0xFF, error message header

        int errorCode = buf.readUnsignedShortLE(); // error code should be unsigned
        String sqlState;

        // Exists only under the protocol 4.1
        if ('#' == buf.getByte(buf.readerIndex())) {
            buf.skipBytes(1); // constant '#'
            sqlState = buf.toString(buf.readerIndex(), SQL_STATE_SIZE, StandardCharsets.US_ASCII);
            buf.skipBytes(SQL_STATE_SIZE); // skip fixed string length by read
        } else {
            sqlState = null;
        }

        return new ErrorMessage(errorCode, sqlState, buf.toString(StandardCharsets.US_ASCII));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ErrorMessage)) {
            return false;
        }

        ErrorMessage that = (ErrorMessage) o;

        return code == that.code && Objects.equals(sqlState, that.sqlState) &&
            message.equals(that.message) && Objects.equals(offendingSql, that.offendingSql);
    }

    @Override
    public int hashCode() {
        int hash = 31 * code + Objects.hashCode(sqlState);
        return 31 * (31 * hash + message.hashCode()) + Objects.hashCode(offendingSql);
    }

    @Override
    public String toString() {
        return "ErrorMessage{code=" + code + ", sqlState='" + sqlState + "', message='" + message + "'}";
    }
}
