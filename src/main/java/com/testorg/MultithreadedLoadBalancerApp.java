package com.testorg;

import com.testorg.healthcheck.HealthCheckManager;
import com.testorg.healthcheck.SimpleHealthChecker;
import com.testorg.loadbalancer.L4LoadBalancer;
import com.testorg.loadbalancer.RoundRobinStrategy;
import com.testorg.loadbalancer.ServerManagerImpl;
import com.testorg.server.Server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MultithreadedLoadBalancerApp - Concurrent Load Balancer Demo (INTENTIONALLY BROKEN!)
 *
 *  Purpose: Demonstrate threading issues in the 1999 scale-up load balancer
 *
 * This version is INTENTIONALLY BROKEN to show what happens when you add
 * multithreading to a single-threaded design without proper thread safety.
 *
 *  Expected Problems:
 * - Round-robin counter corruption (RoundRobinStrategy.currentIndex)
 * - Request distribution imbalances due to race conditions
 * - Server state inconsistencies during concurrent access
 * - Lost updates and data corruption
 *
 *  Demonstration Setup:
 * - 5 worker threads (good for Mac testing)
 * - 25 concurrent "clients" sending requests
 * - Each client sends 10 requests (250 requests total)
 * - Artificial delays to make race conditions more visible
 *
 *
 *  Learning Objectives:
 * 1. See visible race conditions in action
 * 2. Understand why single-threaded code breaks with concurrency
 * 3. Observe unpredictable behavior patterns
 * 4. Motivate the need for thread-safe design
 */
public class MultithreadedLoadBalancerApp {

    // Thread-safe counters for automatic race condition detection
    private static final AtomicInteger totalRequests = new AtomicInteger(0);
    private static final ConcurrentHashMap<String, AtomicInteger> serverRequestCounts = new ConcurrentHashMap<>();

    static {
        serverRequestCounts.put("Server-1", new AtomicInteger(0));
        serverRequestCounts.put("Server-2", new AtomicInteger(0));
        serverRequestCounts.put("Server-3", new AtomicInteger(0));
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Multithreaded Load Balancer Demo (BROKEN VERSION) ===");
        // Create load balancer with explicit dependencies to access strategy for race condition analysis
        ServerManagerImpl serverManager = new ServerManagerImpl();
        RoundRobinStrategy strategy = new RoundRobinStrategy(); // We need reference for race condition detection
        HealthCheckManager healthManager = new HealthCheckManager(serverManager, new SimpleHealthChecker());
        L4LoadBalancer loadBalancer = new L4LoadBalancer(serverManager, strategy, healthManager);

        // Add servers with thread-ID aware names for better observation
        Server server1 = new Server("Server-1");
        Server server2 = new Server("Server-2");
        Server server3 = new Server("Server-3");

        loadBalancer.addServer(server1);
        loadBalancer.addServer(server2);
        loadBalancer.addServer(server3);

        System.out.println(" Created load balancer with 3 servers");
        System.out.println(" Starting health checking...");
        loadBalancer.startHealthChecking();
        Thread.sleep(1000); // Let health checking start

        // Create thread pool - 5 threads for Mac-friendly testing
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        System.out.println(" Thread pool created: 5 worker threads");

        System.out.println("\n Starting concurrent request simulation...");
        System.out.println("Configuration (OPTIMIZED for race condition demonstration):");
        System.out.println("- 10 concurrent clients");
        System.out.println("- 5 requests per client");
        System.out.println("- 50 total requests");
        System.out.println("- Expected per server: ~17 requests (if working correctly)");
        System.out.println("- Fast execution to trigger race conditions");
        System.out.println("\n Request processing starting (watch for race conditions)...\n");

        long startTime = System.currentTimeMillis();

        // Submit 10 concurrent clients, each sending 5 requests (good for race conditions)
        for (int clientId = 1; clientId <= 10; clientId++) {
            final int finalClientId = clientId;

            executorService.submit(() -> {
                // Each client sends 5 requests
                for (int requestId = 1; requestId <= 5; requestId++) {
                    try {
                        String requestName = "Client-" + finalClientId + "-Request-" + requestId;

                        // Short delay to increase race condition probability
                        Thread.sleep(10 + (int)(Math.random() * 50)); // 10-60ms delay

                        // This is where race conditions will occur!
                        String threadName = Thread.currentThread().getName();
                        System.out.println("[" + threadName + "] Processing: " + requestName);

                        // Call the (thread-unsafe) load balancer
                        loadBalancer.distributeRequest(requestName);

                        // Count requests for analysis
                        totalRequests.incrementAndGet();

                        // Short delay after request to increase race condition chances
                        Thread.sleep(20); // 20ms pause between requests

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }

        // Shutdown thread pool and wait for completion (longer timeout for slow execution)
        executorService.shutdown();
        boolean finished = executorService.awaitTermination(60, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // AUTOMATIC race condition analysis using proper APIs (no System.out parsing!)
        System.out.println("\n============================================================");
        System.out.println(" AUTOMATIC RACE CONDITION ANALYSIS");
        System.out.println("============================================================");

        if (finished) {
            System.out.println(" All requests completed in " + duration + "ms");
        } else {
            System.out.println("  Some requests timed out!");
        }

        System.out.println(" Total requests processed: " + totalRequests.get());

        // Query the strategy for race condition detection results (proper API approach!)
        // Cast to access race condition detection methods (specific to broken RoundRobinStrategy)
        boolean raceConditionsFound = strategy.hasRaceConditions();
        int raceConditionCount = strategy.getRaceConditionCount();

        if (raceConditionsFound) {
            System.out.println("\n RACE CONDITIONS DETECTED!");
            System.out.println("Number of race conditions found: " + raceConditionCount);
            System.out.println(" This proves RoundRobinStrategy is NOT thread-safe");
            System.out.println("\n What Happened:");
            System.out.println("- Multiple threads accessed selectServer() simultaneously");
            System.out.println("- Concurrent access to shared currentIndex variable");
            System.out.println("- Lost updates and incorrect server selection occurred");
            System.out.println("\n Race condition demonstration successful!");
        } else {
            System.out.println("\n NO RACE CONDITIONS DETECTED THIS RUN");
            System.out.println(" Race conditions are timing-dependent and may not occur every time");
            System.out.println(" TRY RUNNING AGAIN - Race conditions are intermittent");
            System.out.println(" With more concurrent load, race conditions become more likely");
        }

        // Clean shutdown
        loadBalancer.shutdown();
        System.out.println("\n Demo completed - Race conditions demonstrated!");
    }
}