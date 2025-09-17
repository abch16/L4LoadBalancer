package com.testorg.loadbalancer;

import com.testorg.server.BackendServer;
import com.testorg.server.Server;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ServerManagerTest {

    private ServerManager serverManager;
    private BackendServer server1;
    private BackendServer server2;
    private BackendServer server3;

    @Before
    public void setUp() {
        serverManager = new ServerManagerImpl();
        server1 = new Server("Server-1");
        server2 = new Server("Server-2");
        server3 = new Server("Server-3");
    }

    @Test
    public void testAddServer() {
        serverManager.addServer(server1);
        serverManager.addServer(server2);

        List<BackendServer> allServers = serverManager.getAllServers();
        assertEquals("Should have 2 servers", 2, allServers.size());
        assertTrue("Should contain server1", allServers.contains(server1));
        assertTrue("Should contain server2", allServers.contains(server2));
    }

    @Test
    public void testAddDuplicateServer() {
        serverManager.addServer(server1);
        serverManager.addServer(server1); // Add same server again

        List<BackendServer> allServers = serverManager.getAllServers();
        assertEquals("Should have only 1 server (no duplicates)", 1, allServers.size());
    }

    @Test
    public void testAddNullServer() {
        serverManager.addServer(server1);
        serverManager.addServer(null);

        List<BackendServer> allServers = serverManager.getAllServers();
        assertEquals("Should have only 1 server (null ignored)", 1, allServers.size());
        assertEquals("Should contain server1", server1, allServers.get(0));
    }

    @Test
    public void testRemoveServer() {
        serverManager.addServer(server1);
        serverManager.addServer(server2);

        serverManager.removeServer(server1);

        List<BackendServer> allServers = serverManager.getAllServers();
        assertEquals("Should have 1 server after removal", 1, allServers.size());
        assertTrue("Should contain server2", allServers.contains(server2));
        assertFalse("Should not contain server1", allServers.contains(server1));
    }

    @Test
    public void testGetAvailableServers() {
        serverManager.addServer(server1);
        serverManager.addServer(server2);
        serverManager.addServer(server3);

        // Make server2 unavailable
        server2.setAvailable(false);

        List<BackendServer> availableServers = serverManager.getAvailableServers();
        assertEquals("Should have 2 available servers", 2, availableServers.size());
        assertTrue("Should contain server1", availableServers.contains(server1));
        assertFalse("Should not contain server2", availableServers.contains(server2));
        assertTrue("Should contain server3", availableServers.contains(server3));
    }

    @Test
    public void testHasAvailableServers() {
        // Initially no servers
        assertFalse("Should have no available servers initially", serverManager.hasAvailableServers());

        // Add available server
        serverManager.addServer(server1);
        assertTrue("Should have available servers", serverManager.hasAvailableServers());

        // Make server unavailable
        server1.setAvailable(false);
        assertFalse("Should have no available servers", serverManager.hasAvailableServers());

        // Add another available server
        server2.setAvailable(true);
        serverManager.addServer(server2);
        assertTrue("Should have available servers again", serverManager.hasAvailableServers());
    }

    @Test
    public void testGetAllServersReturnsImmutableCopy() {
        serverManager.addServer(server1);
        serverManager.addServer(server2);

        List<BackendServer> allServers = serverManager.getAllServers();
        int originalSize = allServers.size();

        // Try to modify the returned list
        try {
            allServers.add(server3);
        } catch (UnsupportedOperationException e) {
            // Expected if immutable list is returned
        }

        // Verify original manager is not affected
        List<BackendServer> newAllServers = serverManager.getAllServers();
        assertEquals("Original manager should not be affected", originalSize, newAllServers.size());
    }

    @Test
    public void testServerStateChanges() {
        serverManager.addServer(server1);
        serverManager.addServer(server2);

        // Initially both available
        assertEquals("Should have 2 available servers", 2, serverManager.getAvailableServers().size());

        // Make one unavailable
        server1.setAvailable(false);
        assertEquals("Should have 1 available server", 1, serverManager.getAvailableServers().size());

        // Make it available again
        server1.setAvailable(true);
        assertEquals("Should have 2 available servers again", 2, serverManager.getAvailableServers().size());
    }
}