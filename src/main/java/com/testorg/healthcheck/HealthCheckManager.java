package com.testorg.healthcheck;

import com.testorg.loadbalancer.ServerManager;
import com.testorg.server.BackendServer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * HealthCheckManager - Background Health Monitoring System
 *
 * 🎯 Purpose: Automated server health monitoring for the 1999 scale-up load balancer
 *
 * This component solves a critical production problem: automatically detecting when backend
 * servers become unhealthy and removing them from service WITHOUT requiring manual intervention.
 *
 * 🏗️ Architecture Highlights:
 * - Background Processing: Uses dedicated daemon thread to avoid blocking request handling
 * - Configurable Intervals: Default 5-second checks (tunable for different environments)
 * - Pluggable Health Checkers: Strategy pattern allows different health check implementations
 * - Automatic Recovery: Servers are automatically re-included when they become healthy again
 *
 * 💡 Design Decisions:
 * - Daemon Thread: Doesn't prevent JVM shutdown
 * - Fixed Delay: Ensures consistent gap between health checks (not fixed rate)
 * - Exception Handling: Health check failures automatically mark servers as unhealthy
 * - Graceful Shutdown: Provides clean resource cleanup with timeout
 *
 * 🚀 Health Check Flow:
 * 1. ScheduledExecutorService triggers health checks every 5 seconds
 * 2. Iterate through all servers (healthy and unhealthy)
 * 3. Use pluggable HealthChecker to test each server
 * 4. Update server health status if it changed
 * 5. Log status changes for operational visibility
 *
 * 🔧 Production Benefits:
 * - Automatic Failover: Unhealthy servers are immediately excluded from load balancing
 * - Automatic Recovery: Servers are re-included as soon as they recover
 * - Operational Visibility: Clear logging of health state changes
 * - Resource Cleanup: Proper thread management and shutdown handling
 */
public class HealthCheckManager {
    private final ServerManager serverManager;
    private final HealthChecker healthChecker;
    private final ScheduledExecutorService scheduler;
    private final int intervalSeconds;
    private volatile boolean running;

    public HealthCheckManager(ServerManager serverManager, HealthChecker healthChecker) {
        this(serverManager, healthChecker, 5); // Default 5 seconds
    }

    public HealthCheckManager(ServerManager serverManager, HealthChecker healthChecker, int intervalSeconds) {
        this.serverManager = serverManager;
        this.healthChecker = healthChecker;
        this.intervalSeconds = intervalSeconds;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HealthCheckManager");
            t.setDaemon(true); // Don't prevent JVM shutdown
            return t;
        });
        this.running = false;
    }

    /**
     * Starts the health check manager
     */
    public void start() {
        if (!running) {
            running = true;
            System.out.println("Starting health check manager (interval: " + intervalSeconds + " seconds)");

            scheduler.scheduleWithFixedDelay(
                this::performHealthChecks,
                0, // Start immediately
                intervalSeconds,
                TimeUnit.SECONDS
            );
        }
    }

    /**
     * Stops the health check manager
     */
    public void stop() {
        if (running) {
            running = false;
            System.out.println("Stopping health check manager");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Performs health checks on all servers
     */
    private void performHealthChecks() {
        if (!running) {
            return;
        }

        System.out.println("=== Running health checks ===");

        for (BackendServer server : serverManager.getAllServers()) {
            try {
                boolean currentlyHealthy = healthChecker.isHealthy(server);

                // Update server health status if it changed
                if (server.isHealthy() != currentlyHealthy) {
                    server.setHealthy(currentlyHealthy);
                    String status = currentlyHealthy ? "RECOVERED" : "FAILED";
                    System.out.println("Server " + server.getName() + " health check " + status);
                }

            } catch (Exception e) {
                // If health check throws exception, mark server as unhealthy
                System.out.println("Health check error for " + server.getName() + ": " + e.getMessage());
                server.setHealthy(false);
            }
        }
    }

    public boolean isRunning() {
        return running;
    }
}