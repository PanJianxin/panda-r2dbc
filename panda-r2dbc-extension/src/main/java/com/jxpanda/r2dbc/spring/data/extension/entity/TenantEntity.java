package com.jxpanda.r2dbc.spring.data.extension.entity;

public interface TenantEntity {

    String TENANT_ID = "tenant_id";

    String getTenantId();

    void setTenantId(String tenantId);

}
