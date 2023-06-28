package demo.covert;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.lang.Nullable;

public class RowPropertyAccessor implements PropertyAccessor {

    private final @Nullable RowMetadata rowMetadata;

    RowPropertyAccessor(@Nullable RowMetadata rowMetadata) {
        this.rowMetadata = rowMetadata;
    }

    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class<?>[] { Row.class };
    }

    @Override
    public boolean canRead(EvaluationContext context, @Nullable Object target, String name) {
        return rowMetadata != null && target != null && RowMetadataKit.containsColumn(rowMetadata, name);
    }

    @Override
    public TypedValue read(EvaluationContext context, @Nullable Object target, String name) {

        if (target == null) {
            return TypedValue.NULL;
        }

        Object value = ((Row) target).get(name);

        if (value == null) {
            return TypedValue.NULL;
        }

        return new TypedValue(value);
    }

    @Override
    public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) {
        return false;
    }

    @Override
    public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue) {
        throw new UnsupportedOperationException();
    }
}