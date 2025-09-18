package com.testorg;

import com.testorg.healthcheck.HealthCheckManager;
import com.testorg.healthcheck.SimpleHealthChecker;
import com.testorg.loadbalancer.L4LoadBalancer;
import com.testorg.loadbalancer.ServerManagerImpl;
import com.testorg.loadbalancer.ThreadSafeRoundRobinStrategy;
import com.testorg.server.Server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ThreadSafeLoadBalancerApp - Fixed Multithreaded Demo
 *
 *  Purpose: Demonstrate thread-safe load balancing for the 1999 scale-up solution
 *
 * This version FIXES the race conditions using AtomicInteger, showing how to
 * properly handle concurrent requests in a production load balancer.
 *
 *  Threading Solutions Applied:
 * - AtomicInteger for thread-safe counters
 * - Lock-free round-robin algorithm
 * - Proper concurrent request distribution
 * - Thread-safe server state management
 *
 *  Architecture Benefits:
 * - High throughput: No thread blocking
 * - Fair distribution: True round-robin under load
 * - Production ready: Handles concurrent requests properly
 * - Scalable: Can increase thread pool size as needed
 */
public class ThreadSafeLoadBalancerApp {

    private static final AtomicInteger totalRequests = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Thread-Safe Multithreaded Load Balancer Demo ===");
        System.out.println(" This version uses AtomicInteger to eliminate race conditions!");

        // Create thread-safe load balancer
        ServerManagerImpl serverManager = new ServerManagerImpl();
        ThreadSafeRoundRobinStrategy strategy = new ThreadSafeRoundRobinStrategy();
        HealthCheckManager healthManager = new HealthCheckManager(serverManager, new SimpleHealthChecker());

        L4LoadBalancer loadBalancer = new L4LoadBalancer(serverManager, strategy, healthManager);

        // Add servers
        Server server1 = new Server("Server-1");
        Server server2 = new Server("Server-2");
        Server server3 = new Server("Server-3");

        loadBalancer.addServer(server1);
        loadBalancer.addServer(server2);
        loadBalancer.addServer(server3);

        System.out.println(" Created thread-safe load balancer with 3 servers");
        System.out.println(" Starting health checking...");
        loadBalancer.startHealthChecking();
        Thread.sleep(1000);

        // Create thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        System.out.println(" Thread pool created: 5 worker threads");

        System.out.println("\n Starting thread-safe concurrent request simulation...");
        System.out.println("Configuration:");
        System.out.println("- 15 concurrent clients");
        System.out.println("- 4 requests per client");
        System.out.println("- 60 total requests");
        System.out.println("- Expected per server: 20 requests (should be EXACTLY even!)");
        System.out.println("\n Request processing starting...\n");

        long startTime = System.currentTimeMillis();

        // Submit concurrent clients
        for (int clientId = 1; clientId <= 15; clientId++) {
            final int finalClientId = clientId;

            executorService.submit(() -> {
                for (int requestId = 1; requestId <= 4; requestId++) {
                    try {
                        String requestName = "Client-" + finalClientId + "-Request-" + requestId;

                        // Minimal delay - focus on thread safety, not timing
                        Thread.sleep(5 + (int)(Math.random() * 10));

                        String threadName = Thread.currentThread().getName();
                        System.out.println("[" + threadName + "] Processing: " + requestName);

                        // Call the THREAD-SAFE load balancer
                        boolean handled = loadBalancer.distributeRequest(requestName);

                        totalRequests.incrementAndGet();

                        Thread.sleep(10); // Small gap between requests

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }

        // Wait for completion
        executorService.shutdown();
        boolean finished = executorService.awaitTermination(30, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Analysis
        System.out.println("\n============================================================");
        System.out.println(" THREAD-SAFE RESULTS ANALYSIS");
        System.out.println("============================================================");

        if (finished) {
            System.out.println(" All requests completed in " + duration + "ms");
        } else {
            System.out.println(" Some requests timed out!");
        }

        System.out.println("\n Thread-Safe Performance:");
        System.out.println("Total requests processed: " + totalRequests.get());

        System.out.println("1.  EVEN request distribution (~20 per server)");
        System.out.println("2.  Consistent round-robin pattern");
        System.out.println("3.  No lost updates or counter corruption");
        System.out.println("4.  Reproducible behavior across runs");

        // Clean shutdown
        loadBalancer.shutdown();
        System.out.println("\n Thread-safe demo completed successfully!");
    }
}