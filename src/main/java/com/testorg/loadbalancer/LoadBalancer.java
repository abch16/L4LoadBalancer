package com.testorg.loadbalancer;

import com.testorg.server.BackendServer;

public interface LoadBalancer {
    void addServer(BackendServer server);
    void removeServer(BackendServer server);
    void distributeRequest(String request);
    void setLoadBalancingStrategy(LoadBalancingStrategy strategy);
}