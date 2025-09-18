package com.testorg.loadbalancer;

import com.testorg.server.BackendServer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RoundRobinStrategy - THREAD-UNSAFE Implementation (Intentionally Broken!)
 *
 *
 *  Threading Issues:
 * - currentIndex is not thread-safe (no synchronization)
 * - Multiple threads can read/modify/write simultaneously
 * - Lost updates cause incorrect server selection
 * - Race conditions break round-robin fairness
 *
 *  Expected Problems in Multithreaded Environment:
 * 1. Uneven request distribution (should be ~equal per server)
 * 2. Server selection corruption (wrong servers chosen)
 * 3. Potential ArrayIndexOutOfBoundsException
 * 4. Non-deterministic behavior across runs
 *
 *  This is intentionally broken to demonstrate why thread safety matters!
 */
public class RoundRobinStrategy implements LoadBalancingStrategy {
    //  RACE CONDITION: This field is accessed by multiple threads without synchronization!
    private int currentIndex;

    // Race condition detection (thread-safe counters)
    private final AtomicInteger concurrentAccess = new AtomicInteger(0);
    private volatile boolean raceConditionDetected = false;
    private volatile int detectedRaceConditions = 0;

    public RoundRobinStrategy() {
        this.currentIndex = 0;
    }

    @Override
    public BackendServer selectServer(List<BackendServer> availableServers) {
        if (availableServers == null || availableServers.isEmpty()) {
            return null;
        }

        // Detect concurrent access - increment on entry
        int currentAccess = concurrentAccess.incrementAndGet();

        //  RACE CONDITION DETECTION: If multiple threads are in this method simultaneously
        if (currentAccess > 1) {
            raceConditionDetected = true;
            detectedRaceConditions++;
            String threadName = Thread.currentThread().getName();
            System.out.println(" RACE CONDITION DETECTED! [" + threadName + "] " +
                             currentAccess + " threads accessing selectServer() simultaneously");
        }

        try {
            //  RACE CONDITION ZONE - Multiple threads executing this simultaneously!

            // Step 1: Read currentIndex (Thread A reads 0, Thread B reads 0)
            int indexToUse = currentIndex % availableServers.size();

            // Step 2: Get server (both threads get same server!)
            BackendServer selectedServer = availableServers.get(indexToUse);

            // Step 3: Increment currentIndex (Thread A sets to 1, Thread B overwrites with 1)
            // This is where lost updates occur!
            currentIndex = (currentIndex + 1) % availableServers.size();

            return selectedServer;

        } finally {
            // Decrement on exit (always execute)
            concurrentAccess.decrementAndGet();
        }
    }

    @Override
    public void reset() {
        this.currentIndex = 0;
        // Reset race condition detection
        raceConditionDetected = false;
        detectedRaceConditions = 0;
        concurrentAccess.set(0);
    }

    /**
     * Check if any race conditions were detected during execution
     */
    public boolean hasRaceConditions() {
        return raceConditionDetected;
    }

    /**
     * Get the total number of race conditions detected
     */
    public int getRaceConditionCount() {
        return detectedRaceConditions;
    }
}