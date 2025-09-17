package com.testorg.loadbalancer;

import com.testorg.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;

public class L4LoadBalancerTest {

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
    public void testAddServer() {
        loadBalancer.addServer(server1);
        loadBalancer.addServer(server2);

        // Test by distributing a request - should not get "No servers available" message
        loadBalancer.distributeRequest("Test Request");
        String output = outputStream.toString();
        assertFalse("Should not indicate no servers available",
                   output.contains("No servers available"));
        assertTrue("Should handle request with Server-1",
                  output.contains("Server-1"));
    }

    @Test
    public void testRoundRobinDistribution() {
        loadBalancer.addServer(server1);
        loadBalancer.addServer(server2);
        loadBalancer.addServer(server3);

        // Distribute 6 requests to test round-robin pattern
        for (int i = 1; i <= 6; i++) {
            loadBalancer.distributeRequest("Request " + i);
        }

        String output = outputStream.toString();

        // Each server should handle exactly 2 requests
        int server1Count = countOccurrences(output, "Server-1");
        int server2Count = countOccurrences(output, "Server-2");
        int server3Count = countOccurrences(output, "Server-3");

        assertEquals("Server-1 should handle 2 requests", 2, server1Count);
        assertEquals("Server-2 should handle 2 requests", 2, server2Count);
        assertEquals("Server-3 should handle 2 requests", 2, server3Count);
    }

    @Test
    public void testServerFailover() {
        loadBalancer.addServer(server1);
        loadBalancer.addServer(server2);
        loadBalancer.addServer(server3);

        // Make server2 unavailable
        server2.setAvailable(false);

        // Distribute 4 requests
        for (int i = 1; i <= 4; i++) {
            loadBalancer.distributeRequest("Request " + i);
        }

        String output = outputStream.toString();

        // Only server1 and server3 should handle requests
        assertTrue("Server-1 should handle requests", output.contains("Server-1"));
        assertFalse("Server-2 should not handle requests", output.contains("Server-2"));
        assertTrue("Server-3 should handle requests", output.contains("Server-3"));

        // Verify even distribution between available servers
        int server1Count = countOccurrences(output, "Server-1");
        int server3Count = countOccurrences(output, "Server-3");
        assertEquals("Available servers should handle equal requests", server1Count, server3Count);
    }

    @Test
    public void testAllServersDown() {
        loadBalancer.addServer(server1);
        loadBalancer.addServer(server2);

        // Make all servers unavailable
        server1.setAvailable(false);
        server2.setAvailable(false);

        loadBalancer.distributeRequest("Test Request");

        String output = outputStream.toString();
        assertTrue("Should indicate all servers are down",
                  output.contains("All servers are down"));
        assertTrue("Should mention request could not be handled",
                  output.contains("could not be handled"));
    }

    @Test
    public void testEmptyServerList() {
        // Don't add any servers
        loadBalancer.distributeRequest("Test Request");

        String output = outputStream.toString();
        assertTrue("Should indicate no servers available",
                  output.contains("No servers available"));
    }

    @Test
    public void testServerRecovery() {
        loadBalancer.addServer(server1);
        loadBalancer.addServer(server2);

        // Make server2 unavailable
        server2.setAvailable(false);

        // Clear output from setup
        outputStream.reset();

        // Distribute a request - should go to server1
        loadBalancer.distributeRequest("Request 1");
        String output1 = outputStream.toString();
        assertTrue("Request should go to Server-1", output1.contains("Server-1"));

        // Restore server2
        server2.setAvailable(true);
        outputStream.reset();

        // Next request should go to one of the available servers
        loadBalancer.distributeRequest("Request 2");
        String output2 = outputStream.toString();
        assertTrue("Request should go to an available server after recovery",
                  output2.contains("Server-1") || output2.contains("Server-2"));
    }

    @Test
    public void testPartialServerFailure() {
        loadBalancer.addServer(server1);
        loadBalancer.addServer(server2);
        loadBalancer.addServer(server3);

        // Make server1 and server3 unavailable, keep server2 available
        server1.setAvailable(false);
        server3.setAvailable(false);

        // Distribute multiple requests
        for (int i = 1; i <= 3; i++) {
            loadBalancer.distributeRequest("Request " + i);
        }

        String output = outputStream.toString();

        // Only server2 should handle all requests
        assertFalse("Server-1 should not handle requests", output.contains("Server-1"));
        assertTrue("Server-2 should handle requests", output.contains("Server-2"));
        assertFalse("Server-3 should not handle requests", output.contains("Server-3"));

        // Server-2 should handle all 3 requests
        int server2Count = countOccurrences(output, "Server-2");
        assertEquals("Server-2 should handle all requests", 3, server2Count);
    }

    @Test
    public void testStrategyPatternFunctionality() {
        loadBalancer.addServer(server1);
        loadBalancer.addServer(server2);
        loadBalancer.addServer(server3);

        // Test with default Round Robin strategy
        for (int i = 1; i <= 6; i++) {
            loadBalancer.distributeRequest("RR Request " + i);
        }

        String rrOutput = outputStream.toString();
        outputStream.reset();

        // Switch to Random strategy
        loadBalancer.setLoadBalancingStrategy(new RandomStrategy(123)); // Fixed seed for reproducible results

        // Use more requests to ensure all servers participate
        for (int i = 1; i <= 15; i++) {
            loadBalancer.distributeRequest("Random Request " + i);
        }

        String randomOutput = outputStream.toString();

        // Verify both strategies handled requests
        assertTrue("Round Robin should handle requests", rrOutput.contains("RR Request"));
        assertTrue("Random should handle requests", randomOutput.contains("Random Request"));

        // Verify all servers participated in RR (guaranteed with round-robin)
        assertTrue("All servers should participate in RR",
                  rrOutput.contains("Server-1") && rrOutput.contains("Server-2") && rrOutput.contains("Server-3"));

        // For random strategy, verify at least some requests were handled (with enough requests, likely all servers)
        assertTrue("At least some servers should participate in Random",
                  randomOutput.contains("Server-1") || randomOutput.contains("Server-2") || randomOutput.contains("Server-3"));

        // Count total requests handled by random strategy
        int randomRequestCount = countOccurrences(randomOutput, "handled by server");
        assertEquals("All random requests should be handled", 15, randomRequestCount);
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