package com.larkin.poolExecutor;

import com.larkin.handler.AbortRejectionHandler;
import com.larkin.handler.RejectionHandler;
import com.larkin.queue.CustomBlockingQueue;
import com.larkin.threadFactory.CustomThreadFactory;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MyThreadPoolExecutor implements Executor {
    private final int corePoolSize;
    private final int maxPoolSize;
    private final int keepAliveTime;
    private final TimeUnit timeUnit;
    private final int queueCapacity;
    private final List<Worker> workers = new ArrayList<>();
    private final AtomicInteger workerIdCounter = new AtomicInteger(0);
    private final AtomicInteger activeWorkersCounter = new AtomicInteger(0);
    private volatile boolean isShutdown = false;
    private final CustomThreadFactory threadFactory = new CustomThreadFactory();
    private final Object lock = new Object();
    private RejectionHandler rejectionHandler = new AbortRejectionHandler();

    public MyThreadPoolExecutor(int corePoolSize, int maxPoolSize, int keepAliveTime,
                                TimeUnit timeUnit, int queueCapacity) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.queueCapacity = queueCapacity;
        initializeCoreWorkers();
    }

    public MyThreadPoolExecutor() {
        this(2, 8, 60, TimeUnit.SECONDS, 10);
    }

    public void setRejectionExecutionHandler(RejectionHandler handler) {
        this.rejectionHandler = handler;
    }

    @Override
    public void execute(Runnable task) {
        if (isShutdown) {
            rejectionHandler.rejectedExecution(task, this);
        }

        Worker worker = chooseWorker();
        if (worker.addTask(task)) {
            return;
        }

        synchronized (lock) {
            if (workers.size() < maxPoolSize) {
                Worker newWorker = addWorker(false);
                if (!newWorker.addTask(task)) {
                    rejectionHandler.rejectedExecution(task, this);
                }
            } else {
                rejectionHandler.rejectedExecution(task, this);
            }
        }
    }

    public <T> Future<T> submit(Callable<T> callable) {
        if (callable == null) {
            throw new NullPointerException();
        }
        FutureTask<T> futureTask = new FutureTask<>(callable);
        execute(futureTask);
        return futureTask;
    }

    //    алгоритм Least Loaded
    private Worker chooseWorker() {
        synchronized (lock) {
            Worker leastLoaded = null;
            int minTasks = Integer.MAX_VALUE;

            for (Worker worker : workers) {
                if (!worker.isActive) {
                    return worker;
                }
                int tasks = worker.getCountTasksInQueue();
                if (tasks < minTasks) {
                    minTasks = tasks;
                    leastLoaded = worker;
                }
            }
            return leastLoaded;
        }
    }

    public void shutdown() {
        isShutdown = true;
        synchronized (lock) {
            for (Worker worker : workers) {
                if (!worker.isActive) {
                    worker.thread.interrupt();
                }
            }
        }
    }

    public List<Runnable> shutdownNow() {
        isShutdown = true;
        List<Runnable> remainingTasks = new ArrayList<>();
        synchronized (lock) {
            for (Worker worker : workers) {
                while (!worker.queue.isEmpty()) {
                    remainingTasks.add(worker.queue.poll());
                }
                worker.thread.interrupt();
            }
        }
        return remainingTasks;
    }

    private void initializeCoreWorkers() {
        for (int i = 0; i < corePoolSize; i++) {
            addWorker(true);
        }
    }

    private Worker addWorker(boolean isCore) {
        CustomBlockingQueue<Runnable> queue = new CustomBlockingQueue<>(queueCapacity);
        int id = workerIdCounter.incrementAndGet();
        Worker worker = new Worker(queue, isCore, id);
        Thread thread = threadFactory.newThread(worker);
        synchronized (lock) {
            workers.add(worker);
        }
        thread.start();
        return worker;
    }

    public synchronized int getWorkersCount() {
        return workers.size();
    }

    public int getActiveWorkersCount() {
        return activeWorkersCounter.get();
    }

    private class Worker implements Runnable {
        @Getter
        private final int id;
        private final CustomBlockingQueue<Runnable> queue;
        private final boolean isCore;
        private volatile boolean isActive;
        private volatile Thread thread;

        private Worker(CustomBlockingQueue<Runnable> queue, boolean isCore, int id) {
            this.queue = queue;
            this.isCore = isCore;
            this.id = id;
        }

        @Override
        public void run() {
            this.thread = Thread.currentThread();
            while (true) {
                if (!isCore && isShutdown) {
                    break;
                }
                if (isCore && isShutdown && queue.isEmpty()) {
                    break;
                }
                Runnable task;
                try {
                    task = getTask();
                } catch (InterruptedException e) {
                    thread.interrupt();
                    if (isShutdown) {
                        break;
                    }
                    continue;
                }
                if (task != null) {
                    executeTask(task);
                } else if (!isCore) {
                    System.out.println("[Worker] " + thread.getName() + " idle timeout, stopping.");
                    break;
                }
            }
            synchronized (lock) {
                workers.remove(this);
            }
            System.out.println("[Worker] " + thread.getName() + " terminated.");
        }

        private Runnable getTask() throws InterruptedException {
            if (isCore) {
                return queue.take();
            } else {
                return queue.poll(keepAliveTime, timeUnit);
            }
        }

        private void executeTask(Runnable task) {
            activeWorkersCounter.incrementAndGet();
            isActive = true;
            System.out.println("[Worker] " + thread.getName() + " executes task " + task.toString());
            try {
                task.run();
            } catch (Exception e) {
                System.out.println("Task failed: " + e.getMessage());
            } finally {
                isActive = false;
                activeWorkersCounter.decrementAndGet();
            }
        }

        private boolean addTask(Runnable task) {
            boolean added = queue.offer(task);
            if (added) {
                System.out.println("[Pool] Task accepted into queue for Worker #" + id + ": " + task.toString());
            }
            return added;
        }

        private int getCountTasksInQueue() {
            return queue.size();
        }
    }
}
