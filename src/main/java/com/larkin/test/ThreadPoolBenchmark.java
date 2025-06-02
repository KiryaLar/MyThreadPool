package com.larkin.test;

import com.larkin.poolExecutor.MyThreadPoolExecutor;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolBenchmark {
    private final Executor executor;
    private final int taskCount;
    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private final AtomicInteger rejectedTasks = new AtomicInteger(0);

    public ThreadPoolBenchmark(Executor executor, int taskCount) {
        this.executor = executor;
        this.taskCount = taskCount;
    }

    public void runBenchmark() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < taskCount; i++) {
            try {
                executor.execute(new BenchmarkTask());
            } catch (RejectedExecutionException e) {
                rejectedTasks.incrementAndGet();
            }
        }

        while (completedTasks.get() + rejectedTasks.get() < taskCount) {
            Thread.sleep(100);
        }
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("Elapsed time: " + elapsedTime + " ms");
        System.out.println("Completed tasks: " + completedTasks.get());
        System.out.println("Rejected tasks: " + rejectedTasks.get());
        System.out.println("Average time per task: " + (elapsedTime / (double) completedTasks.get()) + " ms");
    }

    private class BenchmarkTask implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            completedTasks.incrementAndGet();
        }
    }

//    для лучшей наглядности закоммитить все логирование в основных классах
    public static void main(String[] args) throws InterruptedException {
        int[] queueSizes = {2, 10, 25, 35, 50};
        int[] maxPoolSizes = {6, 7, 8};
        int[] corePoolSizes = {2, 3, 4};
        int taskCount = 200;
        int keepAliveTime = 10;

        System.out.println("\n=== Влияние queueSize ===");
        for (int queueSize : queueSizes) {
            System.out.println("\nTesting queue size " + queueSize);
            MyThreadPoolExecutor customExecutor = new MyThreadPoolExecutor(4, 8, keepAliveTime, TimeUnit.SECONDS, queueSize);
            ThreadPoolBenchmark customBenchmark = new ThreadPoolBenchmark(customExecutor, taskCount);
            customBenchmark.runBenchmark();
        }

        System.out.println("\n=== Влияние corePoolSize ===");
        for (int corePoolSize : corePoolSizes) {
            System.out.println("\nTesting corePoolSize = " + corePoolSize);
            MyThreadPoolExecutor customExecutor = new MyThreadPoolExecutor(corePoolSize, 8, keepAliveTime, TimeUnit.SECONDS, 10);
            ThreadPoolBenchmark customBenchmark = new ThreadPoolBenchmark(customExecutor, taskCount);
            customBenchmark.runBenchmark();
        }

        System.out.println("\n=== Влияние maxPoolSize ===");
        for (int maxPoolSize : maxPoolSizes) {
            System.out.println("\nTesting maxPoolSize = " + maxPoolSize);
            MyThreadPoolExecutor customExecutor = new MyThreadPoolExecutor(4, maxPoolSize, keepAliveTime, TimeUnit.SECONDS, 10);
            ThreadPoolBenchmark customBenchmark = new ThreadPoolBenchmark(customExecutor, taskCount);
            customBenchmark.runBenchmark();
        }

        System.out.println("\n=== Сравнение с ThreadPoolExecutor ===");

        System.out.println("\nСтандартный ThreadPoolExecutor");
        ThreadPoolExecutor standardExecutor = new ThreadPoolExecutor(4, 8, keepAliveTime, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10));
        ThreadPoolBenchmark standardBenchmark = new ThreadPoolBenchmark(standardExecutor, taskCount);
        standardBenchmark.runBenchmark();

        System.out.println("\nКастомный MyThreadPoolExecutor");
        MyThreadPoolExecutor customExecutor = new MyThreadPoolExecutor(4, 8, keepAliveTime, TimeUnit.SECONDS, 10);
        ThreadPoolBenchmark customBenchmark = new ThreadPoolBenchmark(customExecutor, taskCount);
        customBenchmark.runBenchmark();
    }
}
