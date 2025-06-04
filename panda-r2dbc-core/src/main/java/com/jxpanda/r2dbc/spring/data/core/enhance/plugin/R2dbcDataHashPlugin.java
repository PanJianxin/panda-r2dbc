package com.jxpanda.r2dbc.spring.data.core.enhance.plugin;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.annotation.R2dbcPluginSlot;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.contract.R2dbcEntityPlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcDataHashSupplier;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcPluginEnum;
import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import reactor.core.publisher.Mono;

@R2dbcPluginSlot(values = {R2dbcPluginEnum.Slot.INSERT})
public class R2dbcDataHashPlugin<T> extends R2dbcOperationPlugin implements R2dbcEntityPlugin<T> {


    private final ReactiveEntityTemplate reactiveEntityTemplate;


    private final R2dbcDataHashSupplier dbcDataHashSupplier;


    public R2dbcDataHashPlugin(ReactiveEntityTemplate reactiveEntityTemplate, R2dbcDataHashSupplier dbcDataHashSupplier) {
        this(R2dbcPluginEnum.Name.DATA_HASH.name(), reactiveEntityTemplate, dbcDataHashSupplier);
    }

    public R2dbcDataHashPlugin(String pluginName, ReactiveEntityTemplate reactiveEntityTemplate, R2dbcDataHashSupplier dbcDataHashSupplier) {
        super(pluginName);
        this.reactiveEntityTemplate = reactiveEntityTemplate;
        this.dbcDataHashSupplier = dbcDataHashSupplier;
    }

    @Override
    public Mono<T> handleEntity(R2dbcPluginEnum.Slot usingSlot, T entity) {
        boolean idEffective = R2dbcMappingKit.isIdEffective(entity);
        // id无效说明是插入操作，直接计算哈希
        if (!idEffective) {
            return dbcDataHashSupplier.hash(entity);
        }
        RelationalPersistentEntity<T> requiredEntity = R2dbcMappingKit.getRequiredEntity(entity);
        Object idValue = R2dbcMappingKit.getPropertyValue(entity, requiredEntity, requiredEntity.getIdProperty());
        return reactiveEntityTemplate.selectById(idValue, requiredEntity.getType())
                .flatMap(oldEntity -> dbcDataHashSupplier.mergeHash(oldEntity, entity));
    }

}
