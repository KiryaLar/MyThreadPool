package com.larkin.handler;

import com.larkin.poolExecutor.MyThreadPoolExecutor;

public interface RejectionHandler {
    void rejectedExecution(Runnable r, MyThreadPoolExecutor executor);
}
