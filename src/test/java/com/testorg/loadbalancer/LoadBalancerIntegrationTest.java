package com.testorg.loadbalancer;

import com.testorg.server.Server;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class LoadBalancerIntegrationTest {

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

        // Add servers to load balancer
        loadBalancer.addServer(server1);
        loadBalancer.addServer(server2);
        loadBalancer.addServer(server3);
    }

    @Test
    public void testCompleteWorkflowFromMainApp() {
        // Phase 1: Distribute initial requests (all should succeed with 3 healthy servers)
        int phase1SuccessCount = 0;
        for (int i = 1; i <= 10; i++) {
            boolean result = loadBalancer.distributeRequest("Request " + i);
            if (result) phase1SuccessCount++;
        }
        assertEquals("All requests in phase 1 should succeed", 10, phase1SuccessCount);

        // Phase 2: Simulate server2 failure (requests should still succeed with server1 and server3)
        server2.setAvailable(false);
        int phase2SuccessCount = 0;
        for (int i = 11; i <= 15; i++) {
            boolean result = loadBalancer.distributeRequest("Request " + i);
            if (result) phase2SuccessCount++;
        }
        assertEquals("All requests in phase 2 should succeed with 2 servers", 5, phase2SuccessCount);

        // Phase 3: Restore server2, fail server3 (requests should still succeed)
        server2.setAvailable(true);
        server3.setAvailable(false);
        int phase3SuccessCount = 0;
        for (int i = 16; i <= 20; i++) {
            boolean result = loadBalancer.distributeRequest("Request " + i);
            if (result) phase3SuccessCount++;
        }
        assertEquals("All requests in phase 3 should succeed with 2 servers", 5, phase3SuccessCount);

        // Phase 4: All servers fail (requests should fail)
        server1.setAvailable(false);
        server2.setAvailable(false);
        server3.setAvailable(false);
        for (int i = 16; i <= 20; i++) {
            boolean result = loadBalancer.distributeRequest("Request " + i);
            assertFalse("Request " + i + " should fail when all servers are down", result);
        }
    }

    @Test
    public void testComplexFailureRecoveryScenario() {
        // Start with all servers available - requests should succeed
        for (int i = 1; i <= 3; i++) {
            boolean result = loadBalancer.distributeRequest("Request " + i);
            assertTrue("Initial requests should succeed", result);
        }

        // Cascade failure: servers fail one by one
        server1.setAvailable(false);
        boolean result1 = loadBalancer.distributeRequest("Request 4");
        assertTrue("Request should succeed with 2 servers available", result1);

        server2.setAvailable(false);
        boolean result2 = loadBalancer.distributeRequest("Request 5");
        assertTrue("Request should succeed with 1 server available", result2);

        server3.setAvailable(false);
        boolean result3 = loadBalancer.distributeRequest("Request 6");
        assertFalse("Request should fail with no servers available", result3);

        // Recovery: servers come back online one by one
        server1.setAvailable(true);
        boolean result4 = loadBalancer.distributeRequest("Request 7");
        assertTrue("Request should succeed after server1 recovery", result4);

        server2.setAvailable(true);
        boolean result5 = loadBalancer.distributeRequest("Request 8");
        assertTrue("Request should succeed with 2 servers recovered", result5);
    }

    @Test
    public void testLoadBalancerUnderStress() {
        int totalRequests = 0;
        int successfulRequests = 0;

        // Simulate high load with frequent server state changes
        for (int round = 0; round < 5; round++) {
            // Randomly fail and recover servers during load
            if (round % 2 == 0) {
                server2.setAvailable(false);
            } else {
                server2.setAvailable(true);
                server3.setAvailable(false);
            }

            // Send batch of requests
            for (int i = 1; i <= 10; i++) {
                boolean result = loadBalancer.distributeRequest("Round " + round + " Request " + i);
                totalRequests++;
                if (result) successfulRequests++;
            }

            // Restore all servers for next round setup
            server2.setAvailable(true);
            server3.setAvailable(true);
        }

        // Verify all requests were processed (some successful, some failed)
        assertEquals("All 50 requests should be processed", 50, totalRequests);
        assertTrue("Some requests should succeed when servers are available", successfulRequests > 0);
        assertTrue("Some requests may fail during server transitions", successfulRequests <= 50);
    }

    @Test
    public void testSingleServerOperationScenario() {
        // Fail all but one server
        server2.setAvailable(false);
        server3.setAvailable(false);

        // Send multiple requests - all should succeed (only server1 available)
        for (int i = 1; i <= 5; i++) {
            boolean result = loadBalancer.distributeRequest("Single Server Request " + i);
            assertTrue("Request " + i + " should be handled by the only available server", result);
        }
    }

    @Test
    public void testAllServersFailure() {
        // Make all servers unavailable
        server1.setAvailable(false);
        server2.setAvailable(false);
        server3.setAvailable(false);

        // All requests should fail
        for (int i = 1; i <= 3; i++) {
            boolean result = loadBalancer.distributeRequest("Failed Request " + i);
            assertFalse("Request " + i + " should fail when all servers are unavailable", result);
        }
    }
}