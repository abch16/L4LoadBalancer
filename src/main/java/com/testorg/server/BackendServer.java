package com.testorg.server;

public interface BackendServer {
    String getName();
    boolean isAvailable();
    void setAvailable(boolean available);
    boolean isHealthy();
    void setHealthy(boolean healthy);
    boolean handleRequest(String request);
}