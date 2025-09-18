package com.testorg.loadbalancer;

import com.testorg.server.Server;
import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.*;

public class L4LoadBalancerTest {

    private L4LoadBalancer loadBalancer;
    private Server server1;
    private Server server2;
    private Server server3;

    @Before
    public void setUp() {
        loadBalancer = new L4LoadBalancer();
        server1 = new Server("Server-1");
        server2 = new Server("Server-2");
        server3 = new Server("Server-3");

    }

    @Test
    public void testAddServer() {
        loadBalancer.addServer(server1);
        loadBalancer.addServer(server2);

        // Test by distributing a request - should return true (success)
        boolean result = loadBalancer.distributeRequest("Test Request");
        assertTrue("Should successfully distribute request to available server", result);
    }

    @Test
    public void testRoundRobinDistribution() {
        loadBalancer.addServer(server1);
        loadBalancer.addServer(server2);
        loadBalancer.addServer(server3);

        // Distribute 6 requests to test round-robin pattern - all should succeed
        for (int i = 1; i <= 6; i++) {
            boolean result = loadBalancer.distributeRequest("Request " + i);
            assertTrue("Request " + i + " should be handled successfully", result);
        }
    }

    @Test
    public void testServerFailover() {
        loadBalancer.addServer(server1);
        loadBalancer.addServer(server2);
        loadBalancer.addServer(server3);

        // Make server2 unavailable
        server2.setAvailable(false);

        // Distribute 4 requests - all should succeed (failover to available servers)
        for (int i = 1; i <= 4; i++) {
            boolean result = loadBalancer.distributeRequest("Request " + i);
            assertTrue("Request " + i + " should be handled by available servers", result);
        }
    }

    @Test
    public void testAllServersDown() {
        loadBalancer.addServer(server1);
        loadBalancer.addServer(server2);

        // Make all servers unavailable
        server1.setAvailable(false);
        server2.setAvailable(false);

        boolean result = loadBalancer.distributeRequest("Test Request");
        assertFalse("Should return false when all servers are down", result);
    }

    @Test
    public void testEmptyServerList() {
        // Don't add any servers
        boolean result = loadBalancer.distributeRequest("Test Request");
        assertFalse("Should return false when no servers available", result);
    }

    @Test
    public void testServerRecovery() {
        loadBalancer.addServer(server1);
        loadBalancer.addServer(server2);

        // Make server2 unavailable
        server2.setAvailable(false);

        // Distribute a request - should succeed (server1 available)
        boolean result1 = loadBalancer.distributeRequest("Request 1");
        assertTrue("Request should be handled by available server", result1);

        // Restore server2
        server2.setAvailable(true);

        // Next request should succeed with both servers available
        boolean result2 = loadBalancer.distributeRequest("Request 2");
        assertTrue("Request should be handled after server recovery", result2);
    }

    @Test
    public void testPartialServerFailure() {
        loadBalancer.addServer(server1);
        loadBalancer.addServer(server2);
        loadBalancer.addServer(server3);

        // Make server1 and server3 unavailable, keep server2 available
        server1.setAvailable(false);
        server3.setAvailable(false);

        // Distribute multiple requests - all should succeed (only server2 available)
        for (int i = 1; i <= 3; i++) {
            boolean result = loadBalancer.distributeRequest("Request " + i);
            assertTrue("Request " + i + " should be handled by available server", result);
        }
    }

    @Test
    public void testStrategyPatternFunctionality() {
        loadBalancer.addServer(server1);
        loadBalancer.addServer(server2);
        loadBalancer.addServer(server3);

        // Test with default Round Robin strategy - all should succeed
        int rrSuccessCount = 0;
        for (int i = 1; i <= 6; i++) {
            boolean result = loadBalancer.distributeRequest("RR Request " + i);
            if (result) rrSuccessCount++;
        }
        assertEquals("All Round Robin requests should succeed", 6, rrSuccessCount);

        // Switch to Random strategy
        loadBalancer.setLoadBalancingStrategy(new RandomStrategy(123)); // Fixed seed for reproducible results

        // Test random strategy - all should succeed
        int randomSuccessCount = 0;
        for (int i = 1; i <= 15; i++) {
            boolean result = loadBalancer.distributeRequest("Random Request " + i);
            if (result) randomSuccessCount++;
        }
        assertEquals("All Random requests should succeed", 15, randomSuccessCount);
    }

}