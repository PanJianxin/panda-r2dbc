package com.jxpanda.r2dbc.spring.data.core.enhance.plugin.annotation;

import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcPluginEnum;

import java.lang.annotation.*;

@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface R2dbcPluginSlot {

    R2dbcPluginEnum.Slot[] values();

}
