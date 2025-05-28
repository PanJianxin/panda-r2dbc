package com.jxpanda.r2dbc.spring.data.extension.entity;

import java.time.LocalDateTime;

public interface AuditableEntity {

    String getCreatorId();

    void setCreatorId(String creatorId);

    String getUpdaterId();

    void setUpdaterId(String updaterId);

    LocalDateTime getCreatedTime();

    void setCreatedTime(LocalDateTime createdTime);

    LocalDateTime getUpdatedTime();

    void setUpdatedTime(LocalDateTime updatedTime);

}
