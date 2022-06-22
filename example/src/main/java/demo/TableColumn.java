package demo;

import org.springframework.core.annotation.AliasFor;
import org.springframework.data.relational.core.mapping.Column;

import java.lang.annotation.*;

@Column
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface TableColumn {

    @AliasFor(annotation = Column.class, attribute = "value")
    String value() default "";

    boolean exists() default true;

    boolean isJson() default false;

    String alias() default "";

}
