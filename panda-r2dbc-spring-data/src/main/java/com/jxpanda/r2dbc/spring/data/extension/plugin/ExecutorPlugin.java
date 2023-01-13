package com.jxpanda.r2dbc.spring.data.extension.plugin;

import org.reactivestreams.Publisher;

public interface ExecutorPlugin<T> {

    Publisher<T> beforeExecute();



    Publisher<T> afterExecuted();

}
