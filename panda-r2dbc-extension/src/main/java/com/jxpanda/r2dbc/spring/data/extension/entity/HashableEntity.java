package com.jxpanda.r2dbc.spring.data.extension.entity;

public interface HashableEntity {

    String getDataHash();

    void setDataHash(String dataHash);

}
