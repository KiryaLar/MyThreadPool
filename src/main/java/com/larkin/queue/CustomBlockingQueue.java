package com.larkin.queue;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class CustomBlockingQueue<E> {
    private final Queue<E> queue = new LinkedList<>();
    private final int capacity;

    public CustomBlockingQueue(int capacity) {
        this.capacity = capacity;
    }

    public int size() {
        return queue.size();
    }

    public synchronized void put(E e) throws InterruptedException {
        while (queue.size() == capacity) {
            wait();
        }
        queue.add(e);
        notifyAll();
    }

    public synchronized E take() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();
        }
        E item = queue.poll();
        notifyAll();
        return item;
    }

    public synchronized boolean offer(E e) {
        if (queue.size() < capacity) {
            queue.add(e);
            notifyAll();
            return true;
        }
        return false;
    }

    public synchronized boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        long deadline = System.nanoTime() + nanos;
        while (queue.size() == capacity) {
            if (nanos <= 0L) return false;
            TimeUnit.NANOSECONDS.timedWait(this, nanos);
            nanos = deadline - System.nanoTime();
        }
        queue.add(e);
        notifyAll();
        return true;
    }

    public synchronized E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        long deadline = System.nanoTime() + nanos;
        while (queue.isEmpty()) {
            if (nanos <= 0L) return null;
            TimeUnit.NANOSECONDS.timedWait(this, nanos);
            nanos = deadline - System.nanoTime();
        }
        E item = queue.poll();
        notifyAll();
        return item;
    }

    public synchronized E poll() {
        return queue.poll();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }
}
