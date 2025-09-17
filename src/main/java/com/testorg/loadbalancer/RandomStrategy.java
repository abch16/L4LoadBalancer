package com.testorg.loadbalancer;

import com.testorg.server.BackendServer;
import java.util.List;
import java.util.Random;

public class RandomStrategy implements LoadBalancingStrategy {
    private final Random random;

    public RandomStrategy() {
        this.random = new Random();
    }

    public RandomStrategy(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public BackendServer selectServer(List<BackendServer> availableServers) {
        if (availableServers == null || availableServers.isEmpty()) {
            return null;
        }

        int randomIndex = random.nextInt(availableServers.size());
        return availableServers.get(randomIndex);
    }

    @Override
    public void reset() {
        // Random strategy doesn't need to reset state
    }
}