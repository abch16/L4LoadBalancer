package com.testorg.loadbalancer;

import com.testorg.server.BackendServer;
import java.util.List;

public class RoundRobinStrategy implements LoadBalancingStrategy {
    private int currentIndex;

    public RoundRobinStrategy() {
        this.currentIndex = 0;
    }

    @Override
    public BackendServer selectServer(List<BackendServer> availableServers) {
        if (availableServers == null || availableServers.isEmpty()) {
            return null;
        }

        BackendServer selectedServer = availableServers.get(currentIndex % availableServers.size());
        currentIndex = (currentIndex + 1) % availableServers.size();
        return selectedServer;
    }

    @Override
    public void reset() {
        this.currentIndex = 0;
    }
}