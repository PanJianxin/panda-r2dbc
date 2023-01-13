package com.jxpanda.r2dbc.spring.data.core.operation;

import org.springframework.data.r2dbc.core.ReactiveSelectOperation;

public interface R2dbcSelectOperation {

    interface R2dbcSelect<T> extends ReactiveSelectOperation.ReactiveSelect<T> {

    }

}
