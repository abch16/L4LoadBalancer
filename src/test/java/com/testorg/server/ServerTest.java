package com.testorg.server;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ServerTest {

    private Server server;

    @Before
    public void setUp() {
        server = new Server("TestServer");
    }

    @Test
    public void testServerInitialization() {
        assertEquals("Server name should be set correctly", "TestServer", server.getName());
        assertTrue("Server should be available by default", server.isAvailable());
        assertTrue("Server should be healthy by default", server.isHealthy());
    }

    @Test
    public void testHandleRequestWhenAvailable() {
        server.setAvailable(true);
        server.setHealthy(true);

        boolean result = server.handleRequest("Test Request");
        assertTrue("Should handle request when available and healthy", result);
    }

    @Test
    public void testHandleRequestWhenUnavailable() {
        server.setAvailable(false);
        server.setHealthy(true);

        boolean result = server.handleRequest("Test Request");
        assertFalse("Should not handle request when unavailable", result);
    }

    @Test
    public void testHandleRequestWhenUnhealthy() {
        server.setAvailable(true);
        server.setHealthy(false);

        boolean result = server.handleRequest("Test Request");
        assertFalse("Should not handle request when unhealthy", result);
    }

    @Test
    public void testGetName() {
        Server namedServer = new Server("MyCustomServer");
        assertEquals("Should return correct server name", "MyCustomServer", namedServer.getName());
    }

    @Test
    public void testMultipleRequests() {
        server.setAvailable(true);
        server.setHealthy(true);

        boolean result1 = server.handleRequest("Request 1");
        boolean result2 = server.handleRequest("Request 2");
        boolean result3 = server.handleRequest("Request 3");

        assertTrue("Should handle Request 1", result1);
        assertTrue("Should handle Request 2", result2);
        assertTrue("Should handle Request 3", result3);
    }

    @Test
    public void testEmptyRequestString() {
        server.setAvailable(true);
        server.setHealthy(true);

        boolean result = server.handleRequest("");
        assertTrue("Should handle empty request", result);
    }

    @Test
    public void testNullRequestString() {
        server.setAvailable(true);
        server.setHealthy(true);

        boolean result = server.handleRequest(null);
        assertTrue("Should handle null request", result);
    }

    @Test
    public void testAvailabilityControl() {
        // Test availability toggle
        assertTrue("Should start available", server.isAvailable());

        server.setAvailable(false);
        assertFalse("Should be unavailable after setting to false", server.isAvailable());

        server.setAvailable(true);
        assertTrue("Should be available after setting to true", server.isAvailable());
    }

    @Test
    public void testHealthControl() {
        // Test health toggle
        assertTrue("Should start healthy", server.isHealthy());

        server.setHealthy(false);
        assertFalse("Should be unhealthy after setting to false", server.isHealthy());

        server.setHealthy(true);
        assertTrue("Should be healthy after setting to true", server.isHealthy());
    }

    @Test
    public void testDualStateLogic() {
        // Test all combinations of availability and health

        // Available and healthy - should handle
        server.setAvailable(true);
        server.setHealthy(true);
        assertTrue("Available + Healthy should handle requests", server.handleRequest("Test"));

        // Available but unhealthy - should not handle
        server.setAvailable(true);
        server.setHealthy(false);
        assertFalse("Available + Unhealthy should not handle requests", server.handleRequest("Test"));

        // Unavailable but healthy - should not handle
        server.setAvailable(false);
        server.setHealthy(true);
        assertFalse("Unavailable + Healthy should not handle requests", server.handleRequest("Test"));

        // Unavailable and unhealthy - should not handle
        server.setAvailable(false);
        server.setHealthy(false);
        assertFalse("Unavailable + Unhealthy should not handle requests", server.handleRequest("Test"));
    }
}