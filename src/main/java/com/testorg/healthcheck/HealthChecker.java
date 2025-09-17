package com.testorg.healthcheck;

import com.testorg.server.BackendServer;

/**
 * Interface for checking the health of backend servers
 */
public interface HealthChecker {
    /**
     * Performs a health check on the given server
     * @param server the server to check
     * @return true if server is healthy, false otherwise
     */
    boolean isHealthy(BackendServer server);
}