package com.jxpanda.r2dbc.spring.data.extension.entity;

import java.time.LocalDateTime;

public interface AuditableEntity {

    String CREATOR_ID = "creator_id";

    String UPDATER_ID = "updater_id";

    String CREATED_TIME = "created_time";

    String UPDATED_TIME = "updated_time";

    String DELETED_TIME = "deleted_time";

    String getCreatorId();

    void setCreatorId(String creatorId);

    String getUpdaterId();

    void setUpdaterId(String updaterId);

    LocalDateTime getCreatedTime();

    void setCreatedTime(LocalDateTime createdTime);

    LocalDateTime getUpdatedTime();

    void setUpdatedTime(LocalDateTime updatedTime);

}
