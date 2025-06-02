package com.larkin.handler;

import com.larkin.poolExecutor.MyThreadPoolExecutor;

import java.util.concurrent.RejectedExecutionException;

public class AbortRejectionHandler implements RejectionHandler {
    @Override
    public void rejectedExecution(Runnable r, MyThreadPoolExecutor executor) {
        System.out.println("[Rejected] Task " + r.toString() + " aborted.");
        throw new RejectedExecutionException("Task rejected");
    }
}
