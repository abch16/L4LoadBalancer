# L4 Load Balancer - Interview Project

## 🛠️ Technology Assumptions

While this is a 1999 scenario, we've made some forward-looking technology choices to demonstrate modern software engineering practices:

- **Java 8**: Despite being "invented in the future," chosen for robust object-oriented design and enterprise readiness
- **Maven**: Build automation and dependency management for scalable development
- **JUnit 4**: Comprehensive testing framework to ensure reliability
- **SOLID Principles**: Clean architecture patterns for maintainable, extensible code

*In 1999, we might have used C++ or early Java, but these choices better demonstrate production-ready system design.*

## 🏗️ Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Client App    │    │  Health Check   │    │ Load Balancing  │
│                 │    │    Manager      │    │   Strategies    │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          ▼                      ▼                      ▼
    ┌─────────────────────────────────────────────────────────────┐
    │                L4LoadBalancer                               │
    │  • Request Distribution                                     │
    │  • Server Health Monitoring                                 │
    │  • Pluggable Load Balancing Algorithms                     │
    └─────────────────────┬───────────────────────────────────────┘
                          │
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │ Server-1 │    │ Server-2 │    │ Server-3 │
    │ Status:  │    │ Status:  │    │ Status:  │
    │ • Health │    │ • Health │    │ • Health │
    │ • Avail  │    │ • Avail  │    │ • Avail  │
    └──────────┘    └──────────┘    └──────────┘
```

## 🧩 Key Components

### Core Load Balancer (`L4LoadBalancer`)
- **Purpose**: Main orchestrator for request distribution
- **Responsibilities**: Integrates health checking, server management, and load balancing strategies
- **Design Pattern**: Dependency injection with SOLID principles

### Health Checking System
- **HealthChecker Interface**: Pluggable health checking strategies
- **HealthCheckManager**: Background monitoring every 5 seconds
- **Health vs Availability**: Separates administrative control from automatic health monitoring

### Load Balancing Strategies
- **Strategy Pattern**: Pluggable algorithms (Round Robin, Random)
- **Extensible**: Easy to add new algorithms (Least Connections, Weighted, etc.)
- **Thread Safe**: Concurrent request handling

### Server Management
- **BackendServer Interface**: Clean abstraction for backend services
- **ServerManager**: Collection management with filtering
- **Dual State**: Both administrative availability and health status

## 🚀 Quick Start

### Prerequisites
- Java 8+
- Maven 3.6+

### Build and Run
```bash
# Compile the project
mvn clean compile

# Run all tests (40 tests covering core functionality and health checking)
mvn test

# Run the interactive demo
mvn exec:java -Dexec.mainClass="com.testorg.SingleThreadLoadBalancerApp"
```

### Demo Walkthrough
The main application demonstrates:

1. **Phase 1**: Normal round-robin load balancing
2. **Phase 2**: Automatic health failure detection and exclusion
3. **Phase 3**: Health recovery and server re-inclusion
4. **Phase 4**: Administrative vs health control separation
5. **Phase 5**: Strategy switching (Round Robin → Random)
6. **Phase 6**: Complete system failure handling

## 📊 Example Output
```
=== L4 Load Balancer with Health Checking Demo ===

Starting health check manager (interval: 5 seconds)
=== Running health checks ===
Health check for Server-1: HEALTHY
Health check for Server-2: HEALTHY
Health check for Server-3: HEALTHY

=== Phase 1: Normal Load Balancing ===
Request "Request 1" handled by server: Server-1
Request "Request 2" handled by server: Server-2
Request "Request 3" handled by server: Server-3

=== Phase 2: Health Check Failure ===
Simulating health failure on Server-2...
Request "Request 7" handled by server: Server-1  // Server-2 excluded
Request "Request 8" handled by server: Server-3
```

## 🧪 Testing Strategy

### Test Coverage (40 tests total)
- **Unit Tests**: Individual component testing
- **Integration Tests**: Health checking system integration
- **Strategy Tests**: Load balancing algorithm verification
- **Edge Cases**: Failure scenarios, recovery, error handling

### Key Test Classes
```bash
# Run specific test suites
mvn test -Dtest=HealthCheckIntegrationTest     # Health checking scenarios
mvn test -Dtest=LoadBalancingStrategyTest      # Algorithm testing
mvn test -Dtest=ServerManagerTest              # Server management
```

## 🏛️ SOLID Principles Implementation

### Single Responsibility Principle
- `L4LoadBalancer`: Request distribution only
- `HealthCheckManager`: Health monitoring only
- `ServerManager`: Server collection management only

### Open/Closed Principle
- New load balancing algorithms can be added without modifying existing code
- New health checking strategies can be plugged in seamlessly

### Liskov Substitution Principle
- All strategies implement their interfaces correctly
- Servers can be substituted without breaking functionality

### Interface Segregation Principle
- Focused interfaces: `LoadBalancingStrategy`, `HealthChecker`, `BackendServer`
- No client depends on methods it doesn't use

### Dependency Inversion Principle
- Depends on abstractions (`ServerManager`, `LoadBalancingStrategy`)
- Not dependent on concrete implementations

## 💡 Design Decisions

### Health vs Availability Separation
```java
// Administrative control (manual)
server.setAvailable(false);

// Health monitoring (automatic)
server.setHealthy(false);

// A server must be BOTH available AND healthy to receive requests
```

### Strategy Pattern for Load Balancing
```java
// Easy algorithm switching
loadBalancer.setLoadBalancingStrategy(new RandomStrategy());
loadBalancer.setLoadBalancingStrategy(new RoundRobinStrategy());
```

### Background Health Monitoring
```java
// Non-blocking health checks every 5 seconds
HealthCheckManager healthManager = new HealthCheckManager(serverManager, healthChecker);
healthManager.start();  // Background thread
```

## 🔧 Extension Points

### Adding New Load Balancing Algorithms
```java
public class LeastConnectionsStrategy implements LoadBalancingStrategy {
    @Override
    public BackendServer selectServer(List<BackendServer> availableServers) {
        // Find server with least active connections
        return serverWithLeastConnections(availableServers);
    }
}
```

### Custom Health Checking
```java
public class HttpHealthChecker implements HealthChecker {
    @Override
    public boolean isHealthy(BackendServer server) {
        // Make HTTP call to /health endpoint
        return makeHttpHealthCheck(server);
    }
}
```

