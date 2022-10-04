package org.springframework.data.r2dbc.core;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;

public class ReactiveSelectOperationAdapter extends ReactiveSelectOperationSupport {

    public ReactiveSelectOperationAdapter(ReactiveEntityTemplate template) {
        super(template);
    }
}
