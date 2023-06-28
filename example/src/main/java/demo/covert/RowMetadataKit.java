package demo.covert;

import io.r2dbc.spi.ColumnMetadata;
import io.r2dbc.spi.RowMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

public class RowMetadataKit {

    private static final @Nullable Method getColumnMetadatas = ReflectionUtils.findMethod(RowMetadata.class,
            "getColumnMetadatas");

    /**
     * Check whether the column {@code name} is contained in {@link RowMetadata}. The check happens case-insensitive.
     *
     * @param metadata the metadata object to inspect.
     * @param name column name.
     * @return {@code true} if the metadata contains the column {@code name}.
     */
    public static boolean containsColumn(RowMetadata metadata, String name) {

        Iterable<? extends ColumnMetadata> columns = getColumnMetadata(metadata);

        for (ColumnMetadata columnMetadata : columns) {
            if (name.equalsIgnoreCase(columnMetadata.getName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return the {@link Iterable} of {@link ColumnMetadata} from {@link RowMetadata}.
     *
     * @param metadata the metadata object to inspect.
     * @return
     * @since 1.4.1
     */
    @SuppressWarnings("unchecked")
    public static Iterable<? extends ColumnMetadata> getColumnMetadata(RowMetadata metadata) {

        if (getColumnMetadatas != null) {
            // Return type of RowMetadata.getColumnMetadatas was updated with R2DBC 0.9.
            return (Iterable<? extends ColumnMetadata>) ReflectionUtils.invokeMethod(getColumnMetadatas, metadata);
        }

        return metadata.getColumnMetadatas();
    }

}
