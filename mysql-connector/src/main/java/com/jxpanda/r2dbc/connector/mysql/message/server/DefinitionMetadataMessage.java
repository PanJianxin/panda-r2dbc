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

import com.jxpanda.r2dbc.connector.mysql.ColumnDefinition;
import com.jxpanda.r2dbc.connector.mysql.ConnectionContext;
import com.jxpanda.r2dbc.connector.mysql.collation.CharCollation;
import com.jxpanda.r2dbc.connector.mysql.util.VarIntUtils;
import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;

import java.nio.charset.Charset;
import java.util.Objects;

import static com.jxpanda.r2dbc.connector.mysql.util.AssertUtils.require;
import static com.jxpanda.r2dbc.connector.mysql.util.AssertUtils.requireNonNull;

/**
 * Column or parameter definition metadata message.
 */
public final class DefinitionMetadataMessage implements ServerMessage {

    @Nullable
    private final String database;

    private final String table;

    @Nullable
    private final String originTable;

    private final String column;

    @Nullable
    private final String originColumn;

    private final int collationId;

    private final long size;

    private final short typeId;

    private final ColumnDefinition definition;

    private final short decimals;

    private DefinitionMetadataMessage(@Nullable String database, String table, @Nullable String originTable,
        String column, @Nullable String originColumn, int collationId, long size, short typeId,
        ColumnDefinition definition, short decimals) {
        require(size >= 0, "size must not be a negative integer");
        require(collationId > 0, "collationId must be a positive integer");

        this.database = database;
        this.table = requireNonNull(table, "table must not be null");
        this.originTable = originTable;
        this.column = requireNonNull(column, "column must not be null");
        this.originColumn = originColumn;
        this.collationId = collationId;
        this.size = size;
        this.typeId = typeId;
        this.definition = requireNonNull(definition, "definition must not be null");
        this.decimals = decimals;
    }

    public String getColumn() {
        return column;
    }

    public int getCollationId() {
        return collationId;
    }

    public long getSize() {
        return size;
    }

    public short getTypeId() {
        return typeId;
    }

    public ColumnDefinition getDefinition() {
        return definition;
    }

    public short getDecimals() {
        return decimals;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefinitionMetadataMessage)) {
            return false;
        }
        DefinitionMetadataMessage that = (DefinitionMetadataMessage) o;
        return collationId == that.collationId &&
            size == that.size &&
            typeId == that.typeId &&
            definition.equals(that.definition) &&
            decimals == that.decimals &&
            Objects.equals(database, that.database) &&
            table.equals(that.table) &&
            Objects.equals(originTable, that.originTable) &&
            column.equals(that.column) &&
            Objects.equals(originColumn, that.originColumn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(database, table, originTable, column, originColumn, collationId, size, typeId,
            definition, decimals);
    }

    @Override
    public String toString() {
        return "DefinitionMetadataMessage{database='" + database + "', table='" + table + "' (origin:'" +
            originTable + "'), column='" + column + "' (origin:'" + originColumn + "'), collationId=" +
            collationId + ", size=" + size + ", type=" + typeId + ", definition=" + definition +
            ", decimals=" + decimals + '}';
    }

    static DefinitionMetadataMessage decode(ByteBuf buf, ConnectionContext context) {
        if (context.getCapability().isProtocol41()) {
            return decode41(buf, context);
        }

        return decode320(buf, context);
    }

    private static DefinitionMetadataMessage decode320(ByteBuf buf, ConnectionContext context) {
        CharCollation collation = context.getClientCollation();
        Charset charset = collation.getCharset();
        String table = readVarIntSizedString(buf, charset);
        String column = readVarIntSizedString(buf, charset);

        buf.skipBytes(1); // Constant 0x3
        int size = buf.readUnsignedMediumLE();

        buf.skipBytes(1); // Constant 0x1
        short typeId = buf.readUnsignedByte();

        buf.skipBytes(1); // Constant 0x3
        ColumnDefinition definition = ColumnDefinition.of(buf.readShortLE());
        short decimals = buf.readUnsignedByte();

        return new DefinitionMetadataMessage(null, table, null, column, null, collation.getId(), size, typeId,
            definition, decimals);
    }

    private static DefinitionMetadataMessage decode41(ByteBuf buf, ConnectionContext context) {
        buf.skipBytes(4); // "def" which sized by var integer

        CharCollation collation = context.getClientCollation();
        Charset charset = collation.getCharset();
        String database = readVarIntSizedString(buf, charset);
        String table = readVarIntSizedString(buf, charset);
        String originTable = readVarIntSizedString(buf, charset);
        String column = readVarIntSizedString(buf, charset);
        String originColumn = readVarIntSizedString(buf, charset);

        VarIntUtils.readVarInt(buf); // skip constant 0x0c encoded by var integer

        int collationId = buf.readUnsignedShortLE();
        long size = buf.readUnsignedIntLE();
        short typeId = buf.readUnsignedByte();
        ColumnDefinition definition = ColumnDefinition.of(buf.readShortLE());

        return new DefinitionMetadataMessage(database, table, originTable, column, originColumn, collationId,
            size, typeId, definition, buf.readUnsignedByte());
    }

    private static String readVarIntSizedString(ByteBuf buf, Charset charset) {
        // JVM can NOT support string which length upper than maximum of int32
        int bytes = (int) VarIntUtils.readVarInt(buf);

        if (bytes == 0) {
            return "";
        }

        String result = buf.toString(buf.readerIndex(), bytes, charset);
        buf.skipBytes(bytes);

        return result;
    }
}
