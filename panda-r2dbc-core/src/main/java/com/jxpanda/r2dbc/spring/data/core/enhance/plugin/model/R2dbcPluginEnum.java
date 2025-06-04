package com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model;

import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcAuditingPlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcDataHashPlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcLogicDeletePlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcTenantPlugin;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * @author Panda
 */

public class R2dbcPluginEnum {

    @Getter
    @RequiredArgsConstructor
    public enum Name {
        /**
         * 逻辑删除插件
         */
        LOGIC_DELETE(100, "逻辑删除插件", R2dbcLogicDeletePlugin.class),
        /**
         * 审计插件
         */
        AUDITING(200, "审计插件", R2dbcAuditingPlugin.class),
        /**
         * 多租户插件
         */
        TENANT(300, "多租户插件", R2dbcTenantPlugin.class),
        /**
         * 数据哈希插件
         */
        DATA_HASH(400, "数据哈希插件", R2dbcDataHashPlugin.class);

        /**
         * 默认排序
         */
        private final Integer defaultSort;

        /**
         * 描述
         */
        private final String description;

        /**
         * 默认实现类
         */
        private final Class<?> defaultImplClass;
    }

    @Getter
    @RequiredArgsConstructor
    public enum Slot {

        /**
         * 插入前
         */
        INSERT(new Class[]{R2dbcInsertExecutor.class, R2dbcSaveExecutor.class}),

        /**
         * 更新前
         */
        UPDATE(new Class[]{R2dbcUpdateExecutor.class, R2dbcSaveExecutor.class}),

        /**
         * 删除前
         */
        DELETE(new Class[]{R2dbcDeleteExecutor.class, R2dbcDestroyExecutor.class}),

        /**
         * 查询前
         */
        SELECT(new Class[]{R2dbcSelectExecutor.class});


        private final Class<? extends R2dbcOperationExecutor<?, ?>>[] bindingExecutors;

        public boolean isSupport(Class<? extends R2dbcOperationExecutor<?, ?>> executorClass){
            return Arrays.stream(this.bindingExecutors).anyMatch(executorClass::isAssignableFrom);
        }

        public static Slot fromExecutor(Class<? extends R2dbcOperationExecutor<?, ?>> executorClass){
            // TODO: 这里有一个隐藏的隐患，关于R2dbcSaveExecutor的，是界定为INSERT还是UPDATE
            //  暂时先这么写，按优先级来说的话，应该是界定与INSERT
            //  但是实际上不会存在这种情况，因为R2dbcSaveExecutor最终会swap为R2dbcInsertExecutor或者R2dbcUpdateExecutor其中之一来执行操作
            //  所以，理论上R2dbcOperationExecutor只会传入R2dbcInsertExecutor、R2dbcUpdateExecutor、R2dbcDeleteExecutor、R2dbcSelectExecutor四个类型
            for (Slot slot : Slot.values()) {
                if (slot.isSupport(executorClass)) {
                    return slot;
                }
            }
            return null;
        }

    }

}
