package com.larkin;

import com.larkin.poolExecutor.MyThreadPoolExecutor;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        MyThreadPoolExecutor executor = new MyThreadPoolExecutor(
                2,
                4,
                5,
                TimeUnit.SECONDS,
                1);

        System.out.println("Submitting 10 tasks...");
        for (int i = 1; i <= 10; i++) {
            try {
                executor.execute(new CustomTask(i));
//                System.out.println("--------------- " + executor.getActiveWorkersCount() + " ---------------");
            } catch (RejectedExecutionException e) {
                System.out.println("CustomTask " + i + " rejected: " + e.getMessage());
            }
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

//        System.out.println("--------------- " + executor.getActiveWorkersCount() + " ---------------");

        System.out.println("Submitting one more task...");
        try {
            executor.execute(new CustomTask(11));
        } catch (RejectedExecutionException e) {
            System.out.println("CustomTask 11 rejected: " + e.getMessage());
        }

        System.out.println("Initiating shutdown...");
        executor.shutdown();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Pool workers count: " + executor.getWorkersCount());
        System.out.println("Pool active workers: " + executor.getActiveWorkersCount());
        if (executor.getWorkersCount() == 0 && executor.getActiveWorkersCount() == 0) {
            System.out.println("All tasks completed, and threads released.");
        } else {
            System.out.println("Some tasks or threads are still active.");
        }
    }

    static class CustomTask implements Runnable {
        private final int id;

        public CustomTask(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            System.out.println("[CustomTask] " + this + " started by" + Thread.currentThread().getName());
            try {
                Thread.sleep(1000); //Имитация работы
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("[CustomTask] " + this + " completed by" + Thread.currentThread().getName());
        }

        @Override
        public String toString() {
            return "CustomTask-" + id;
        }
    }
}