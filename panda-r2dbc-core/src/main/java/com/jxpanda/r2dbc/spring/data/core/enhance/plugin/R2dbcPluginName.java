package com.jxpanda.r2dbc.spring.data.core.enhance.plugin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Panda
 */

@Getter
@RequiredArgsConstructor
public enum R2dbcPluginName {

    /**
     * 逻辑删除插件
     */
    LOGIC_DELETE(Group.CRITERIA, "LOGIC_DELETE", "逻辑删除插件", R2DbcLogicDeletePlugin.class);


    /**
     * 分组
     * */
    private final String group;

    /**
     * 插件名
     */
    private final String name;

    /**
     * 描述
     */
    private final String description;

    /**
     * 默认实现类
     */
    private final Class<?> defaultImplClass;


    public static final class Group {
        public static final String CRITERIA = "CRITERIA";

    }

}
