package com.jxpanda.r2dbc.spring.data.extension.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableId;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author Panda
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class StandardEntity<ID> implements Entity<ID> {

    /**
     * 主键ID
     */
    @TableId
    @JsonSerialize(using = ToStringSerializer.class)
    private ID id;


}
