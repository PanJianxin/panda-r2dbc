package com.jxpanda.r2dbc.spring.data.extension.entity;

public interface OptimisticLockEntity {

    Integer getVersion();

    void setVersion(Integer version);

}
