package com.testorg.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;

public class ServerTest {

    private Server server;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @Before
    public void setUp() {
        server = new Server("TestServer");

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
    public void testServerInitialization() {
        assertEquals("Server name should be set correctly", "TestServer", server.getName());
        assertTrue("Server should be available by default", server.isAvailable());
    }


    @Test
    public void testHandleRequestWhenAvailable() {
        server.setAvailable(true);
        server.handleRequest("Test Request");

        String output = outputStream.toString();
        assertTrue("Should contain request text", output.contains("Test Request"));
        assertTrue("Should contain server name", output.contains("TestServer"));
        assertTrue("Should indicate request handled", output.contains("handled by server"));
    }


    @Test
    public void testGetName() {
        Server namedServer = new Server("MyCustomServer");
        assertEquals("Should return correct server name", "MyCustomServer", namedServer.getName());
    }

    @Test
    public void testMultipleRequests() {
        server.setAvailable(true);

        server.handleRequest("Request 1");
        server.handleRequest("Request 2");
        server.handleRequest("Request 3");

        String output = outputStream.toString();

        // Count occurrences of each request
        assertTrue("Should handle Request 1", output.contains("Request 1"));
        assertTrue("Should handle Request 2", output.contains("Request 2"));
        assertTrue("Should handle Request 3", output.contains("Request 3"));

        // Should have 3 instances of "handled by server"
        int handledCount = countOccurrences(output, "handled by server");
        assertEquals("Should handle all 3 requests", 3, handledCount);
    }


    @Test
    public void testEmptyRequestString() {
        server.setAvailable(true);
        server.handleRequest("");

        String output = outputStream.toString();
        assertTrue("Should handle empty request", output.contains("handled by server"));
        assertTrue("Should contain server name", output.contains("TestServer"));
    }

    @Test
    public void testNullRequestString() {
        server.setAvailable(true);
        server.handleRequest(null);

        String output = outputStream.toString();
        assertTrue("Should handle null request", output.contains("handled by server"));
        assertTrue("Should contain server name", output.contains("TestServer"));
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