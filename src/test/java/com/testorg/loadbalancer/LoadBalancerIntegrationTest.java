package com.testorg.loadbalancer;

import com.testorg.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;

public class LoadBalancerIntegrationTest {

    private L4LoadBalancer loadBalancer;
    private Server server1;
    private Server server2;
    private Server server3;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

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

        // Capture System.out for testing
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    @After
    public void tearDown() {
        // Restore original System.out
        System.setOut(originalOut);
    }

    @Test
    public void testCompleteWorkflowFromMainApp() {
        // Phase 1: Distribute initial requests (simulating requests 1-10 from main app)
        for (int i = 1; i <= 10; i++) {
            loadBalancer.distributeRequest("Request " + i);
        }

        String phase1Output = outputStream.toString();

        // Verify round-robin distribution in phase 1
        int server1Count = countOccurrences(phase1Output, "Server-1");
        int server2Count = countOccurrences(phase1Output, "Server-2");
        int server3Count = countOccurrences(phase1Output, "Server-3");

        // With 10 requests and 3 servers: should be 4, 3, 3 or 3, 4, 3 or 3, 3, 4
        assertEquals("Total requests in phase 1 should be 10", 10, server1Count + server2Count + server3Count);
        assertTrue("Server distribution should be fairly balanced",
                  Math.abs(server1Count - server2Count) <= 1 &&
                  Math.abs(server2Count - server3Count) <= 1);

        // Phase 2: Simulate server2 failure (requests 11-15 from main app)
        outputStream.reset();
        server2.setAvailable(false);

        for (int i = 11; i <= 15; i++) {
            loadBalancer.distributeRequest("Request " + i);
        }

        String phase2Output = outputStream.toString();

        // Verify only server1 and server3 handle requests
        assertTrue("Server-1 should handle requests in phase 2", phase2Output.contains("Server-1"));
        assertFalse("Server-2 should not handle requests in phase 2", phase2Output.contains("Server-2"));
        assertTrue("Server-3 should handle requests in phase 2", phase2Output.contains("Server-3"));

        // Phase 3: Restore server2, fail server3 (requests 16-20 from main app)
        outputStream.reset();
        server2.setAvailable(true);
        server3.setAvailable(false);

        for (int i = 16; i <= 20; i++) {
            loadBalancer.distributeRequest("Request " + i);
        }

        String phase3Output = outputStream.toString();

        // Verify only server1 and server2 handle requests
        assertTrue("Server-1 should handle requests in phase 3", phase3Output.contains("Server-1"));
        assertTrue("Server-2 should handle requests in phase 3", phase3Output.contains("Server-2"));
        assertFalse("Server-3 should not handle requests in phase 3", phase3Output.contains("Server-3"));

        // Phase 4: All servers fail (requests 16-20 again from main app)
        outputStream.reset();
        server1.setAvailable(false);
        server2.setAvailable(false);
        server3.setAvailable(false);

        for (int i = 16; i <= 20; i++) {
            loadBalancer.distributeRequest("Request " + i);
        }

        String phase4Output = outputStream.toString();

        // Verify all requests show "All servers are down"
        int downMessages = countOccurrences(phase4Output, "All servers are down");
        assertEquals("Should have 5 'all servers down' messages", 5, downMessages);
        assertFalse("No servers should handle requests in phase 4",
                   phase4Output.contains("handled by server"));
    }

    @Test
    public void testComplexFailureRecoveryScenario() {
        // Start with all servers available
        for (int i = 1; i <= 3; i++) {
            loadBalancer.distributeRequest("Request " + i);
        }

        String initialOutput = outputStream.toString();
        assertTrue("All servers should be active initially",
                  initialOutput.contains("Server-1") &&
                  initialOutput.contains("Server-2") &&
                  initialOutput.contains("Server-3"));

        // Cascade failure: servers fail one by one
        outputStream.reset();
        server1.setAvailable(false);

        loadBalancer.distributeRequest("Request 4");
        loadBalancer.distributeRequest("Request 5");

        server2.setAvailable(false);

        loadBalancer.distributeRequest("Request 6");
        loadBalancer.distributeRequest("Request 7");

        String cascadeOutput = outputStream.toString();

        // Verify requests 4-5 go to server2/3, requests 6-7 go to server3 only
        assertFalse("Server-1 should not handle requests after failure", cascadeOutput.contains("Server-1"));
        assertTrue("Server-3 should handle requests throughout cascade", cascadeOutput.contains("Server-3"));

        // Recovery: servers come back online one by one
        outputStream.reset();
        server1.setAvailable(true);

        loadBalancer.distributeRequest("Request 8");
        loadBalancer.distributeRequest("Request 9");

        server2.setAvailable(true);

        loadBalancer.distributeRequest("Request 10");
        loadBalancer.distributeRequest("Request 11");

        String recoveryOutput = outputStream.toString();

        // Verify gradual recovery
        assertTrue("Server-1 should handle requests after recovery", recoveryOutput.contains("Server-1"));
        assertTrue("Server-2 should handle requests after recovery", recoveryOutput.contains("Server-2"));
        assertTrue("Server-3 should continue handling requests", recoveryOutput.contains("Server-3"));
    }

    @Test
    public void testLoadBalancerUnderStress() {
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
                loadBalancer.distributeRequest("Round " + round + " Request " + i);
            }

            // Restore all servers for next round setup
            server2.setAvailable(true);
            server3.setAvailable(true);
        }

        String stressOutput = outputStream.toString();

        // Verify no requests were lost (should see all 50 requests processed)
        int totalProcessed = countOccurrences(stressOutput, "handled by server");
        int totalFailed = countOccurrences(stressOutput, "All servers are down");

        // All requests should either be handled or explicitly marked as failed
        assertEquals("All 50 requests should be accounted for", 50, totalProcessed + totalFailed);

        // Verify all available servers participated
        assertTrue("Server-1 should have participated", stressOutput.contains("Server-1"));
        assertTrue("Server-3 should have participated", stressOutput.contains("Server-3"));
    }

    @Test
    public void testSingleServerOperationScenario() {
        // Fail all but one server
        server2.setAvailable(false);
        server3.setAvailable(false);

        // Send multiple requests
        for (int i = 1; i <= 5; i++) {
            loadBalancer.distributeRequest("Single Server Request " + i);
        }

        String output = outputStream.toString();

        // Verify only server1 handles all requests
        int server1Count = countOccurrences(output, "Server-1");
        assertEquals("Server-1 should handle all 5 requests", 5, server1Count);
        assertFalse("Server-2 should not handle any requests", output.contains("Server-2"));
        assertFalse("Server-3 should not handle any requests", output.contains("Server-3"));
    }

    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}