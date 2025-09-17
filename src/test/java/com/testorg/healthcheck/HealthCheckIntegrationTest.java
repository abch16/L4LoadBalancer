package com.testorg.healthcheck;

import com.testorg.loadbalancer.L4LoadBalancer;
import com.testorg.loadbalancer.ServerManagerImpl;
import com.testorg.loadbalancer.RoundRobinStrategy;
import com.testorg.server.BackendServer;
import com.testorg.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;

public class HealthCheckIntegrationTest {

    private L4LoadBalancer loadBalancer;
    private BackendServer server1;
    private BackendServer server2;
    private BackendServer server3;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

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

        // Capture System.out for testing
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    @After
    public void tearDown() {
        // Restore original System.out and shutdown load balancer
        System.setOut(originalOut);
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

        // Wait for health check to detect the failure
        Thread.sleep(100); // Short wait for health check to run

        // Clear output and test load balancing
        outputStream.reset();
        for (int i = 1; i <= 6; i++) {
            loadBalancer.distributeRequest("Request " + i);
        }

        String output = outputStream.toString();

        // Only server1 and server3 should handle requests
        assertTrue("Server-1 should handle requests", output.contains("Server-1"));
        assertFalse("Server-2 should not handle requests due to health failure", output.contains("Server-2"));
        assertTrue("Server-3 should handle requests", output.contains("Server-3"));
    }

    @Test
    public void testHealthRecovery() throws InterruptedException {
        // Start health checking
        loadBalancer.startHealthChecking();

        // Simulate server failure and recovery
        server2.setHealthy(false);
        Thread.sleep(50); // Wait for failure detection

        // Clear output and verify server2 is excluded
        outputStream.reset();
        loadBalancer.distributeRequest("Request during failure");
        String failureOutput = outputStream.toString();
        assertFalse("Server-2 should not handle requests when unhealthy", failureOutput.contains("Server-2"));

        // Recover server2
        server2.setHealthy(true);
        Thread.sleep(50); // Wait for recovery detection

        // Clear output and verify server2 is included again
        outputStream.reset();
        for (int i = 1; i <= 6; i++) {
            loadBalancer.distributeRequest("Recovery Request " + i);
        }

        String recoveryOutput = outputStream.toString();
        assertTrue("Server-2 should handle requests after recovery", recoveryOutput.contains("Server-2"));
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

        // Clear output and test load balancing
        outputStream.reset();
        loadBalancer.distributeRequest("Request with all servers down");

        String output = outputStream.toString();
        assertTrue("Should indicate all servers are down", output.contains("All servers are down"));
        assertFalse("No server should handle the request", output.contains("handled by server"));
    }

    @Test
    public void testHealthVsAvailabilityDifference() throws InterruptedException {
        // Start health checking
        loadBalancer.startHealthChecking();

        // Test 1: Server available but unhealthy
        server1.setAvailable(true);
        server1.setHealthy(false);

        outputStream.reset();
        loadBalancer.distributeRequest("Test unhealthy server");
        String output1 = outputStream.toString();
        assertFalse("Unhealthy server should not receive requests", output1.contains("Server-1"));

        // Test 2: Server unavailable but healthy
        server2.setAvailable(false);
        server2.setHealthy(true);

        outputStream.reset();
        loadBalancer.distributeRequest("Test unavailable server");
        String output2 = outputStream.toString();
        assertFalse("Unavailable server should not receive requests", output2.contains("Server-2"));

        // Test 3: Server available and healthy
        server3.setAvailable(true);
        server3.setHealthy(true);

        outputStream.reset();
        loadBalancer.distributeRequest("Test healthy available server");
        String output3 = outputStream.toString();
        assertTrue("Available and healthy server should receive requests", output3.contains("Server-3"));
    }

    @Test
    public void testHealthCheckLogging() throws InterruptedException {
        // Start health checking
        loadBalancer.startHealthChecking();

        // Make server1 unhealthy to trigger health check logging
        outputStream.reset();
        server1.setHealthy(false);

        // Wait for a few health check cycles
        Thread.sleep(100);

        String output = outputStream.toString();
        assertTrue("Should contain health check logging",
                  output.contains("Health check") || output.contains("health checks"));
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