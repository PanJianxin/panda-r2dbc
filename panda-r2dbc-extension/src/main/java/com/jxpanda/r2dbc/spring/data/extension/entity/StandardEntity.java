package com.jxpanda.r2dbc.spring.data.extension.entity;

import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableId;
import com.jxpanda.r2dbc.spring.data.core.enhance.strategy.ValidationStrategy;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.util.ObjectUtils;

/**
 * 标准entity对象
 *
 * @author Panda
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class StandardEntity implements Entity {

    /**
     * 主键ID
     */
    @TableId(validationPolicy = ValidationStrategy.NOT_EMPTY)
    private String id;

    @Override
    public boolean isEffective() {
        return !ObjectUtils.isEmpty(getId());
    }


}
