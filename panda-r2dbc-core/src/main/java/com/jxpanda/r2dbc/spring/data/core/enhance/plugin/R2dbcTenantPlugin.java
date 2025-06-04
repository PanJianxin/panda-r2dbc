package com.jxpanda.r2dbc.spring.data.core.enhance.plugin;

import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.annotation.R2dbcPluginSlot;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.contract.R2dbcCriteriaPlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.contract.R2dbcEntityPlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcPluginEnum;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcTenantIdSupplier;
import com.jxpanda.r2dbc.spring.data.core.kit.QueryKit;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.ReflectionKit;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import reactor.core.publisher.Mono;

import java.util.Objects;

@R2dbcPluginSlot(values = {R2dbcPluginEnum.Slot.INSERT, R2dbcPluginEnum.Slot.SELECT, R2dbcPluginEnum.Slot.DELETE})
public class R2dbcTenantPlugin<T> extends R2dbcOperationPlugin implements R2dbcEntityPlugin<T>, R2dbcCriteriaPlugin<T> {

    private static final String DEFAULT_TENANT_ID_FIELD_NAME = "tenantId";

    private final R2dbcTenantIdSupplier tenantIdSupplier;

    private final String tenantIdFieldName;

    public R2dbcTenantPlugin(R2dbcTenantIdSupplier tenantIdSupplier) {
        this(tenantIdSupplier, DEFAULT_TENANT_ID_FIELD_NAME);
    }

    public R2dbcTenantPlugin(R2dbcTenantIdSupplier tenantIdSupplier, String tenantIdFieldName) {
        this(R2dbcPluginEnum.Name.TENANT.name(), tenantIdSupplier, tenantIdFieldName);
    }

    public R2dbcTenantPlugin(String pluginName, R2dbcTenantIdSupplier tenantIdSupplier, String tenantIdFieldName) {
        super(pluginName);
        this.tenantIdSupplier = tenantIdSupplier;
        this.tenantIdFieldName = Objects.requireNonNull(tenantIdFieldName);
    }


    @Override
    public Mono<CriteriaDefinition> handleCriteria(Class<T> entityType, CriteriaDefinition original) {
        return QueryKit.criteriaMono(original)
                .zipWith(tenantIdSupplier.get())
                .map(tuple -> {
                    CriteriaDefinition criteriaDefinition = tuple.getT1();
                    String tenantId = tuple.getT2();
                    return QueryKit.mergeCriteria(criteriaDefinition, Criteria.where(tenantIdFieldName).is(tenantId));
                });
    }

    @Override
    public Mono<T> handleEntity(R2dbcPluginEnum.Slot usingSlot, T entity) {
        return tenantIdSupplier.get()
                .map(tenantId -> {
                    ReflectionKit.setFieldValue(entity, tenantIdFieldName, tenantId);
                    return entity;
                });
    }
}