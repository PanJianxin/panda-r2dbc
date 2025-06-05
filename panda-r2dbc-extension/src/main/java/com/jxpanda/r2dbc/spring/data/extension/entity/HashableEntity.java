package com.jxpanda.r2dbc.spring.data.extension.entity;

public interface HashableEntity {

    String DATA_HASH = "data_hash";

    String getDataHash();

    void setDataHash(String dataHash);

}
