package com.jxpanda.r2dbc.spring.data.core.enhance.plugin;

import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.annotation.R2dbcPluginSlot;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.contract.R2dbcEntityPlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.contract.R2dbcUpdatePlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcAuditingIdSupplier;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcPluginEnum;
import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.ReflectionKit;
import org.springframework.data.relational.core.query.Update;
import reactor.core.publisher.Mono;

import java.util.Objects;

@R2dbcPluginSlot(values = {R2dbcPluginEnum.Slot.INSERT, R2dbcPluginEnum.Slot.UPDATE})
public class R2dbcAuditingPlugin<T> extends R2dbcOperationPlugin implements R2dbcEntityPlugin<T>, R2dbcUpdatePlugin<T> {


    private static final String DEFAULT_CREATOR_ID_FIELD_NAME = "creatorId";
    private static final String DEFAULT_UPDATER_ID_FIELD_NAME = "updaterId";

    private final R2dbcAuditingIdSupplier auditingIdSupplier;

    private final String creatorIdFieldName;

    private final String updaterIdFiledName;

    public R2dbcAuditingPlugin(R2dbcAuditingIdSupplier auditingIdSupplier) {
        this(auditingIdSupplier, DEFAULT_CREATOR_ID_FIELD_NAME, DEFAULT_UPDATER_ID_FIELD_NAME);
    }

    public R2dbcAuditingPlugin(R2dbcAuditingIdSupplier auditingIdSupplier, String creatorIdFieldName, String updaterIdFiledName) {
        this(R2dbcPluginEnum.Name.AUDITING.name(), auditingIdSupplier, creatorIdFieldName, updaterIdFiledName);
    }

    public R2dbcAuditingPlugin(String pluginName, R2dbcAuditingIdSupplier auditingIdSupplier, String creatorIdFieldName, String updaterIdFiledName) {
        super(pluginName);
        this.auditingIdSupplier = auditingIdSupplier;
        this.creatorIdFieldName = Objects.requireNonNull(creatorIdFieldName);
        this.updaterIdFiledName = Objects.requireNonNull(updaterIdFiledName);
    }

    @Override
    public Mono<T> handleEntity(R2dbcPluginEnum.Slot usingSlot, T entity) {
        if (R2dbcPluginEnum.Slot.INSERT == usingSlot) {
            return Mono.zip(setCreatorId(entity), setUpdaterId(entity))
                    .map(values -> entity);
        }
        return setUpdaterId(entity);
    }

    @Override
    public Mono<Update> handleUpdate(Class<T> entityType, Update original) {
        return auditingIdSupplier.getUpdaterId()
                .map(updaterId -> original == null ? Update.update(updaterIdFiledName, updaterId) : original.set(updaterIdFiledName, updaterId));
    }

    private Mono<T> setCreatorId(T entity) {
        return auditingIdSupplier.getCreatorId()
                .map(creatorId -> {
                    ReflectionKit.setFieldValue(entity, creatorIdFieldName, creatorId);
                    return entity;
                });
    }

    private Mono<T> setUpdaterId(T entity) {
        return auditingIdSupplier.getUpdaterId()
                .map(updaterId -> {
                    ReflectionKit.setFieldValue(entity, updaterIdFiledName, updaterId);
                    return entity;
                });
    }

}
