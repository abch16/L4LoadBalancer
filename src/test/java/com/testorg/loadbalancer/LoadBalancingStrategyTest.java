package com.testorg.loadbalancer;

import com.testorg.server.BackendServer;
import com.testorg.server.Server;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class LoadBalancingStrategyTest {

    private List<BackendServer> servers;
    private BackendServer server1;
    private BackendServer server2;
    private BackendServer server3;

    @Before
    public void setUp() {
        server1 = new Server("Server-1");
        server2 = new Server("Server-2");
        server3 = new Server("Server-3");

        servers = new ArrayList<>();
        servers.add(server1);
        servers.add(server2);
        servers.add(server3);
    }

    @Test
    public void testRoundRobinStrategy() {
        RoundRobinStrategy strategy = new RoundRobinStrategy();

        // Test round-robin distribution
        BackendServer first = strategy.selectServer(servers);
        BackendServer second = strategy.selectServer(servers);
        BackendServer third = strategy.selectServer(servers);
        BackendServer fourth = strategy.selectServer(servers); // Should wrap around

        assertEquals("First selection should be Server-1", server1, first);
        assertEquals("Second selection should be Server-2", server2, second);
        assertEquals("Third selection should be Server-3", server3, third);
        assertEquals("Fourth selection should wrap to Server-1", server1, fourth);
    }

    @Test
    public void testRoundRobinStrategyReset() {
        RoundRobinStrategy strategy = new RoundRobinStrategy();

        // Make some selections
        strategy.selectServer(servers);
        strategy.selectServer(servers);

        // Reset strategy
        strategy.reset();

        // Next selection should start from beginning
        BackendServer selected = strategy.selectServer(servers);
        assertEquals("After reset, should start from Server-1", server1, selected);
    }

    @Test
    public void testRandomStrategyWithFixedSeed() {
        RandomStrategy strategy = new RandomStrategy(42); // Fixed seed for reproducible results

        // Make multiple selections and verify they're valid servers
        for (int i = 0; i < 10; i++) {
            BackendServer selected = strategy.selectServer(servers);
            assertNotNull("Selected server should not be null", selected);
            assertTrue("Selected server should be in the list", servers.contains(selected));
        }
    }

    @Test
    public void testRandomStrategyDistribution() {
        RandomStrategy strategy = new RandomStrategy(123);

        // Track selections over many iterations
        int[] counts = new int[3];
        int iterations = 300;

        for (int i = 0; i < iterations; i++) {
            BackendServer selected = strategy.selectServer(servers);
            if (selected == server1) counts[0]++;
            else if (selected == server2) counts[1]++;
            else if (selected == server3) counts[2]++;
        }

        // Each server should get roughly 1/3 of requests (allowing for randomness)
        int expectedPerServer = iterations / 3;
        int tolerance = expectedPerServer / 2; // 50% tolerance

        assertTrue("Server-1 should get reasonable distribution",
                  counts[0] > expectedPerServer - tolerance && counts[0] < expectedPerServer + tolerance);
        assertTrue("Server-2 should get reasonable distribution",
                  counts[1] > expectedPerServer - tolerance && counts[1] < expectedPerServer + tolerance);
        assertTrue("Server-3 should get reasonable distribution",
                  counts[2] > expectedPerServer - tolerance && counts[2] < expectedPerServer + tolerance);
    }

    @Test
    public void testStrategyWithEmptyList() {
        RoundRobinStrategy rrStrategy = new RoundRobinStrategy();
        RandomStrategy randomStrategy = new RandomStrategy();

        List<BackendServer> emptyList = new ArrayList<>();

        assertNull("Round robin with empty list should return null",
                  rrStrategy.selectServer(emptyList));
        assertNull("Random with empty list should return null",
                  randomStrategy.selectServer(emptyList));
    }

    @Test
    public void testStrategyWithNullList() {
        RoundRobinStrategy rrStrategy = new RoundRobinStrategy();
        RandomStrategy randomStrategy = new RandomStrategy();

        assertNull("Round robin with null list should return null",
                  rrStrategy.selectServer(null));
        assertNull("Random with null list should return null",
                  randomStrategy.selectServer(null));
    }

    @Test
    public void testStrategyWithSingleServer() {
        RoundRobinStrategy rrStrategy = new RoundRobinStrategy();
        RandomStrategy randomStrategy = new RandomStrategy();

        List<BackendServer> singleServer = new ArrayList<>();
        singleServer.add(server1);

        // Both strategies should return the single server
        assertEquals("Round robin with single server", server1, rrStrategy.selectServer(singleServer));
        assertEquals("Random with single server", server1, randomStrategy.selectServer(singleServer));
    }
}