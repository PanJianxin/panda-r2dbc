package com.jxpanda.r2dbc.spring.data.extension.entity;

public interface OptimisticLockEntity {

    String VERSION = "version";

    Integer getVersion();

    void setVersion(Integer version);

}
