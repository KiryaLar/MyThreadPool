package com.larkin.threadFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadFactory implements ThreadFactory {

    private final AtomicInteger threadNumber = new AtomicInteger(1);

    public Thread newThread(Runnable r) {
        int num = threadNumber.getAndIncrement();
        Thread thread = new Thread(r);
        thread.setName("Worker №" + num);
        System.out.println("[Thread Factory] Creating new thread: " + thread.getName());
        return thread;
    }
}
