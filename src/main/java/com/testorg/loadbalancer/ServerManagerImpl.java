package com.testorg.loadbalancer;

import com.testorg.server.BackendServer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ServerManagerImpl implements ServerManager {
    private final List<BackendServer> servers;

    public ServerManagerImpl() {
        this.servers = new ArrayList<>();
    }

    @Override
    public void addServer(BackendServer server) {
        if (server != null && !servers.contains(server)) {
            servers.add(server);
        }
    }

    @Override
    public void removeServer(BackendServer server) {
        servers.remove(server);
    }

    @Override
    public List<BackendServer> getAvailableServers() {
        return servers.stream()
                .filter(server -> server.isAvailable() && server.isHealthy())
                .collect(Collectors.toList());
    }

    @Override
    public List<BackendServer> getAllServers() {
        return new ArrayList<>(servers);
    }

    @Override
    public boolean hasAvailableServers() {
        return servers.stream().anyMatch(server -> server.isAvailable() && server.isHealthy());
    }
}