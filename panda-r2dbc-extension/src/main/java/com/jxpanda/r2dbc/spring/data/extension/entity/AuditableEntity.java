package com.jxpanda.r2dbc.spring.data.extension.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableLogic;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.value.LogicDeleteValueType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AuditableEntity extends StandardEntity<String> {

    @TableColumn(name = "creator_id")
    private String creatorId;

    @TableColumn(name = "updater_id")
    private String updaterId;

    @TableColumn(name = "created_time")
    private LocalDateTime createdTime;

    @TableColumn(name = "updated_time")
    private LocalDateTime updatedTime;

    @JsonIgnore
    @TableColumn(name = "deleted_time")
    @TableLogic(type = LogicDeleteValueType.DATE_TIME)
    private LocalDateTime deletedTime;

    @JsonIgnore
    @TableColumn(name = "version")
    private Integer version;


    @Override
    public boolean isEffective() {
        return super.isEffective() && !getId().equals("0");
    }
}
