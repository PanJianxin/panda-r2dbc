package com.jxpanda.r2dbc.spring.data.core.enhance.query.page;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;

@Data
@Setter
@AllArgsConstructor
public class Paging implements Page {


    private long current;
    private int size;
    private boolean queryCount;

    public Paging(long current, int size) {
        this(current, size, true);
    }

}
