package com.testorg.loadbalancer;

import com.testorg.server.BackendServer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ThreadSafeRoundRobinStrategy - Thread-Safe Implementation
 *
 *  THREAD-SAFE: Uses AtomicInteger to prevent race conditions!
 *
 *  Threading Solutions:
 * - AtomicInteger for thread-safe counter operations
 * - getAndIncrement() provides atomic read-modify-write
 * - No need for synchronized methods (better performance)
 */
public class ThreadSafeRoundRobinStrategy implements LoadBalancingStrategy {
    //  THREAD-SAFE: AtomicInteger prevents race conditions
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    public ThreadSafeRoundRobinStrategy() {
        // AtomicInteger initializes to 0 by default
    }

    @Override
    public BackendServer selectServer(List<BackendServer> availableServers) {
        if (availableServers == null || availableServers.isEmpty()) {
            return null;
        }

        String threadName = Thread.currentThread().getName();

        //  THREAD-SAFE ZONE - AtomicInteger handles concurrency correctly!

        // Atomic operation: get current value and increment in one step
        int indexToUse = currentIndex.getAndIncrement() % availableServers.size();

        // Get selected server
        BackendServer selectedServer = availableServers.get(indexToUse);

        // Show current state for educational purposes
        System.out.println("[" + threadName + "] ThreadSafeRoundRobin: selected " + selectedServer.getName() +
                          " (index=" + indexToUse + ", currentIndex now=" + currentIndex.get() + ")");

        return selectedServer;
    }

    @Override
    public void reset() {
        currentIndex.set(0);
        System.out.println("[" + Thread.currentThread().getName() + "] ThreadSafeRoundRobin: reset currentIndex to 0");
    }

    /**
     * Get current index value (for testing/debugging)
     */
    public int getCurrentIndex() {
        return currentIndex.get();
    }
}