package com.testorg.loadbalancer;

import com.testorg.server.BackendServer;
import com.testorg.healthcheck.HealthCheckManager;
import com.testorg.healthcheck.SimpleHealthChecker;

/**
 * L4LoadBalancer - Main Load Balancer Implementation
 *
 * This is the core orchestrator for the 1999 scale-up load balancing solution.
 * Implements a production-ready Layer 4 load balancer with the following capabilities:
 *
 * 🎯 Core Requirements (1999 Scale-up Scenario):
 * - Accept traffic from many clients ✅
 * - Balance traffic across multiple backend services ✅
 * - Remove services from operation if they go offline ✅
 *
 * 🏗️ Architecture Highlights:
 * - SOLID Principles: Clean separation of concerns with dependency injection
 * - Strategy Pattern: Pluggable load balancing algorithms (Round Robin, Random, etc.)
 * - Health Monitoring: Automatic 5-second health checks with graceful failover
 * - Dual State Management: Separates administrative availability from health status
 *
 * 💡 Design Decisions:
 * - Health checking is mandatory (not optional) for production reliability
 * - Background health monitoring prevents blocking request distribution
 * - Strategy pattern allows runtime algorithm switching without downtime
 *
 * 🚀 Usage Flow:
 * 1. Create load balancer (health checking included by default)
 * 2. Add backend servers
 * 3. Start health checking
 * 4. Distribute requests (automatic failover/recovery)
 * 5. Optionally switch strategies at runtime
 * 6. Graceful shutdown
 */
public class L4LoadBalancer implements LoadBalancer {
    // Core components following Single Responsibility Principle
    private final ServerManager serverManager;          // Manages server collections and filtering
    private LoadBalancingStrategy strategy;             // Pluggable algorithm (Strategy Pattern)
    private final HealthCheckManager healthCheckManager; // Background health monitoring

    /**
     * Full Constructor - Dependency Injection (Recommended for Production)
     *
     * Allows complete control over dependencies for testing and customization.
     * This constructor follows Dependency Inversion Principle - depends on
     * abstractions, not concrete implementations.
     */
    public L4LoadBalancer(ServerManager serverManager, LoadBalancingStrategy strategy, HealthCheckManager healthCheckManager) {
        this.serverManager = serverManager;
        this.strategy = strategy;
        this.healthCheckManager = healthCheckManager;
    }

    /**
     * Default Constructor - Production Ready Defaults
     *
     * Creates a load balancer with sensible defaults:
     * - Round Robin strategy (fair distribution)
     * - Simple health checker (basic TCP-style checks)
     * - 5-second health check interval
     *
     * Perfect for the 1999 scale-up scenario - works out of the box!
     */
    public L4LoadBalancer() {
        this.serverManager = new ServerManagerImpl();;
        this.strategy = new RoundRobinStrategy();
        this.healthCheckManager = new HealthCheckManager(serverManager, new SimpleHealthChecker());
    }

    @Override
    public void addServer(BackendServer server) {
        serverManager.addServer(server);
    }

    @Override
    public void removeServer(BackendServer server) {
        serverManager.removeServer(server);
    }

    /**
     * Core Request Distribution Logic
     *
     * This is where the magic happens! The main entry point for all client requests.
     *
     * 🎯 Algorithm:
     * 1. Check if any servers are available (both healthy AND administratively available)
     * 2. If no servers available, gracefully reject request with appropriate message
     * 3. Use pluggable strategy to select best server from available pool
     * 4. Forward request to selected server
     *
     * 💡 Key Design Decisions:
     * - Fail-fast: Immediate rejection if no servers available (don't queue/retry)
     * - Strategy Pattern: Algorithm selection is decoupled from request handling
     * - Dual State Filtering: Only servers that are BOTH available AND healthy can serve
     * - Graceful Degradation: System continues operating with partial server failures
     *
     * 🚀 Performance Characteristics:
     * - O(1) availability check (ServerManager handles filtering efficiently)
     * - O(n) strategy selection (where n = number of healthy servers)
     * - Non-blocking: Health checks run in background, don't delay requests
     */
    @Override
    public void distributeRequest(String request) {
        // First check: Do we have any servers that can handle requests?
        // A server must be BOTH administratively available AND healthy
        if (!serverManager.hasAvailableServers()) {
            // Provide different messages for different failure scenarios
            if (serverManager.getAllServers().isEmpty()) {
                System.out.println("No servers available to handle the request!");
            } else {
                System.out.println("All servers are down! Request \"" + request + "\" could not be handled.");
            }
            return;
        }

        // Use pluggable strategy to select the best server from available pool
        // This is where Round Robin, Random, Least Connections, etc. algorithms plug in
        BackendServer selectedServer = strategy.selectServer(serverManager.getAvailableServers());

        if (selectedServer != null) {
            // Forward the request to the selected server
            // Server handles the actual request processing
            selectedServer.handleRequest(request);
        }
    }

    @Override
    public void setLoadBalancingStrategy(LoadBalancingStrategy strategy) {
        this.strategy = strategy;
        if (this.strategy != null) {
            this.strategy.reset();
        }
    }

    /**
     * Starts health checking
     */
    public void startHealthChecking() {
        healthCheckManager.start();
    }

    /**
     * Stops health checking
     */
    public void stopHealthChecking() {
        healthCheckManager.stop();
    }

    /**
     * Returns true if health checking is running
     */
    public boolean isHealthCheckingEnabled() {
        return healthCheckManager.isRunning();
    }

    /**
     * Shutdown the load balancer and stop all background processes
     */
    public void shutdown() {
        stopHealthChecking();
    }
}