package com.example.liars_bar.common;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Bir xil kalitli vazifalar ketma-ket (kelgan tartibda), turli kalitlilar parallel bajariladi.
 * Navbatlar chegaralangan: to'lib ketsa, vazifa tashlab yuboriladi.
 * Ishchi oqimlar virtual: tarmoqni kutish platforma oqimini band qilmaydi.
 */
@Slf4j
public final class PartitionedExecutor implements AutoCloseable {

    private final String name;
    private final ThreadPoolExecutor[] partitions;

    public PartitionedExecutor(String name, int partitionCount, int queueCapacity) {
        this.name = name;
        this.partitions = new ThreadPoolExecutor[partitionCount];
        for (int i = 0; i < partitionCount; i++) {
            String threadName = name + "-" + i;
            partitions[i] = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(queueCapacity),
                    Thread.ofVirtual().name(threadName).factory());
        }
    }

    /** @return vazifa qabul qilinsa true */
    public boolean execute(long key, Runnable task) {
        ThreadPoolExecutor executor = partitions[Math.floorMod(Long.hashCode(key), partitions.length)];
        try {
            executor.execute(() -> {
                try {
                    task.run();
                } catch (RuntimeException e) {
                    log.error("Task failed in {}", name, e);
                }
            });
            return true;
        } catch (RejectedExecutionException e) {
            log.warn("{} queue is full, task dropped", name);
            return false;
        }
    }

    @Override
    public void close() {
        for (ThreadPoolExecutor executor : partitions) {
            executor.shutdown();
        }
        for (ThreadPoolExecutor executor : partitions) {
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
