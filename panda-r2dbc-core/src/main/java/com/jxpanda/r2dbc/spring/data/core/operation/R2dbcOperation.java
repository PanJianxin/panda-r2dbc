package com.jxpanda.r2dbc.spring.data.core.operation;

import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcOperationOption;

/**
 * @author Panda
 */
public interface R2dbcOperation<T, S extends R2dbcOperation<T, S>> {

    S withOption(R2dbcOperationOption option);

}
