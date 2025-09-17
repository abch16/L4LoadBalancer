package com.testorg.healthcheck;

import com.testorg.server.BackendServer;

/**
 * Simple health checker that checks the server's current health status
 * In a real implementation, this would make network calls to test connectivity
 */
public class SimpleHealthChecker implements HealthChecker {

    @Override
    public boolean isHealthy(BackendServer server) {
        if (server == null) {
            return false;
        }

        // In a real implementation, this would:
        // - Try to establish a TCP connection to the server
        // - Make an HTTP call to a health endpoint
        // - Check response time and validate the response

        // For our simulation, we simply check the server's health status
        boolean healthy = server.isHealthy();

        // Log the health check result
        System.out.println("Health check for " + server.getName() + ": " +
                          (healthy ? "HEALTHY" : "UNHEALTHY"));

        return healthy;
    }
}