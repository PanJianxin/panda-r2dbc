package com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Cmd {
    NONE("不做任何处理"),
    MINE("自动注入userId，只查询自己的数据");

    private final String description;
}
