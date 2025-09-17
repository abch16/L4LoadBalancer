package com.testorg.loadbalancer;

import com.testorg.server.BackendServer;
import java.util.List;

public interface LoadBalancingStrategy {
    BackendServer selectServer(List<BackendServer> availableServers);
    void reset();
}