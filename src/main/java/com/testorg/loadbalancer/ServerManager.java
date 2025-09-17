package com.testorg.loadbalancer;

import com.testorg.server.BackendServer;
import java.util.List;

public interface ServerManager {
    void addServer(BackendServer server);
    void removeServer(BackendServer server);
    List<BackendServer> getAvailableServers();
    List<BackendServer> getAllServers();
    boolean hasAvailableServers();
}