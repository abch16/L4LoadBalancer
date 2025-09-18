package com.testorg.healthcheck;

import com.testorg.loadbalancer.L4LoadBalancer;
import com.testorg.loadbalancer.ServerManagerImpl;
import com.testorg.loadbalancer.RoundRobinStrategy;
import com.testorg.server.BackendServer;
import com.testorg.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class HealthCheckIntegrationTest {

    private L4LoadBalancer loadBalancer;
    private BackendServer server1;
    private BackendServer server2;
    private BackendServer server3;

    @Before
    public void setUp() {
        // Create load balancer with health checking
        ServerManagerImpl serverManager = new ServerManagerImpl();
        HealthCheckManager healthManager = new HealthCheckManager(serverManager, new SimpleHealthChecker());
        loadBalancer = new L4LoadBalancer(serverManager, new RoundRobinStrategy(), healthManager);

        // Create servers
        server1 = new Server("Server-1");
        server2 = new Server("Server-2");
        server3 = new Server("Server-3");

        // Add servers to load balancer
        loadBalancer.addServer(server1);
        loadBalancer.addServer(server2);
        loadBalancer.addServer(server3);
    }

    @After
    public void tearDown() {
        loadBalancer.shutdown();
    }

    @Test
    public void testHealthCheckManagerStartStop() {
        assertFalse("Health checking should start as stopped", loadBalancer.isHealthCheckingEnabled());

        loadBalancer.startHealthChecking();
        assertTrue("Health checking should be enabled after start", loadBalancer.isHealthCheckingEnabled());

        loadBalancer.stopHealthChecking();
        assertFalse("Health checking should be disabled after stop", loadBalancer.isHealthCheckingEnabled());
    }

    @Test
    public void testHealthFailureDetection() throws InterruptedException {
        // Start health checking
        loadBalancer.startHealthChecking();

        // All servers should be healthy initially
        assertTrue("Server1 should be healthy", server1.isHealthy());
        assertTrue("Server2 should be healthy", server2.isHealthy());
        assertTrue("Server3 should be healthy", server3.isHealthy());

        // Simulate server2 health failure
        server2.setHealthy(false);
        Thread.sleep(100); // Wait for health check to detect the failure

        // Test load balancing - all requests should still succeed (server1 and server3 available)
        for (int i = 1; i <= 6; i++) {
            boolean result = loadBalancer.distributeRequest("Request " + i);
            assertTrue("Request " + i + " should be handled by healthy servers", result);
        }
    }

    @Test
    public void testHealthRecovery() throws InterruptedException {
        // Start health checking
        loadBalancer.startHealthChecking();

        // Simulate server failure and recovery
        server2.setHealthy(false);
        Thread.sleep(50); // Wait for failure detection

        // Test request during failure - should still succeed (server1 and server3 available)
        boolean failureResult = loadBalancer.distributeRequest("Request during failure");
        assertTrue("Request should be handled by healthy servers during failure", failureResult);

        // Recover server2
        server2.setHealthy(true);
        Thread.sleep(50); // Wait for recovery detection

        // Test requests after recovery - all should succeed with all 3 servers healthy
        for (int i = 1; i <= 6; i++) {
            boolean result = loadBalancer.distributeRequest("Recovery Request " + i);
            assertTrue("Recovery Request " + i + " should be handled successfully", result);
        }
    }

    @Test
    public void testAllServersUnhealthy() throws InterruptedException {
        // Start health checking
        loadBalancer.startHealthChecking();

        // Make all servers unhealthy
        server1.setHealthy(false);
        server2.setHealthy(false);
        server3.setHealthy(false);
        Thread.sleep(50); // Wait for health check detection

        // Test load balancing - should fail when no servers are healthy
        boolean result = loadBalancer.distributeRequest("Request with all servers down");
        assertFalse("Request should fail when all servers are unhealthy", result);
    }

    @Test
    public void testHealthVsAvailabilityDifference() throws InterruptedException {
        // Start health checking
        loadBalancer.startHealthChecking();

        // Test 1: Server available but unhealthy
        server1.setAvailable(true);
        server1.setHealthy(false);

        boolean result1 = loadBalancer.distributeRequest("Test unhealthy server");
        assertTrue("Request should be handled by other healthy servers", result1);

        // Test 2: Server unavailable but healthy
        server2.setAvailable(false);
        server2.setHealthy(true);

        boolean result2 = loadBalancer.distributeRequest("Test unavailable server");
        assertTrue("Request should be handled by available and healthy servers", result2);

        // Test 3: Server available and healthy
        server3.setAvailable(true);
        server3.setHealthy(true);

        boolean result3 = loadBalancer.distributeRequest("Test healthy available server");
        assertTrue("Available and healthy server should handle requests", result3);
    }

    @Test
    public void testHealthCheckLogging() throws InterruptedException {
        // Start health checking
        loadBalancer.startHealthChecking();

        // Make server1 unhealthy to trigger health check logging
        server1.setHealthy(false);

        // Wait for a few health check cycles
        Thread.sleep(100);

        // Verify system continues to work
        boolean result = loadBalancer.distributeRequest("Test during health check");
        assertTrue("System should continue working during health checks", result);
    }

    @Test
    public void testLoadBalancerShutdown() {
        // Start health checking
        loadBalancer.startHealthChecking();
        assertTrue("Health checking should be enabled", loadBalancer.isHealthCheckingEnabled());

        // Shutdown should stop health checking
        loadBalancer.shutdown();
        assertFalse("Health checking should be disabled after shutdown", loadBalancer.isHealthCheckingEnabled());
    }
}