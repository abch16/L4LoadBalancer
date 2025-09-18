package com.testorg;

import com.testorg.loadbalancer.L4LoadBalancer;
import com.testorg.loadbalancer.RandomStrategy;
import com.testorg.server.Server;

/**
 * SingleThreadLoadBalancerApp - Interactive Demo Application
 *
 *  Purpose: Comprehensive demonstration of the 1999 scale-up load balancer solution
 *
 * This demo showcases all the key features that solve the original problem:
 * "A rapidly growing scale-up has outgrown its single server setup. Things are failing.
 *  Build a load balancer to handle the load across multiple machines."
 *
 *  Demo Scenarios:
 * Phase 1: Normal Operations - Round robin load balancing across healthy servers
 * Phase 2: Health Failures - Automatic server exclusion when health checks fail
 * Phase 3: Health Recovery - Automatic server re-inclusion when health is restored
 * Phase 4: Administrative Control - Manual server management vs automatic health monitoring
 * Phase 5: Strategy Switching - Runtime algorithm changes (Round Robin → Random)
 * Phase 6: Complete Failure - Graceful handling when all servers are down
 *
 *  Key Demonstrations:
 * - Automatic failover and recovery (no manual intervention required)
 * - Clear separation between administrative and health state management
 * - Pluggable algorithms that can be switched at runtime
 * - Production-ready error handling and logging
 * - Proper resource cleanup and shutdown procedures
 */
public class SingleThreadLoadBalancerApp
{
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== L4 Load Balancer with Health Checking Demo ===");
        System.out.println("Demonstrating the 1999 scale-up load balancing solution...\n");

        // Create the Load Balancer using default constructor
        // This automatically includes health checking - no more single points of failure!
        L4LoadBalancer loadBalancer = new L4LoadBalancer();
        System.out.println(" Load balancer created with health checking enabled by default");

        // Initialize the server pool - these represent the backend services
        // that need to handle the increased load from our growing scale-up
        Server server1 = new Server("Server-1");
        Server server2 = new Server("Server-2");
        Server server3 = new Server("Server-3");

        // Register servers with the load balancer
        // Each server starts as both available (admin control) and healthy (automatic)
        loadBalancer.addServer(server1);
        loadBalancer.addServer(server2);
        loadBalancer.addServer(server3);
        System.out.println(" Added 3 backend servers to the load balancer");

        // Start the background health monitoring system
        // This solves the "remove services if they go offline" requirement automatically
        System.out.println("\n🔍 Starting background health monitoring (5-second intervals)...");
        loadBalancer.startHealthChecking();
        Thread.sleep(1000); // Allow health check system to initialize

        // =========================================================================
        // PHASE 1: NORMAL OPERATIONS
        // Demonstrates: Basic round-robin load balancing across healthy servers
        // Expected: Each server handles 2 requests in round-robin order
        // =========================================================================
        System.out.println("\n=== Phase 1: Normal Load Balancing ===");
        System.out.println(" Demonstrating: Round-robin distribution across 3 healthy servers");

        for (int i = 1; i <= 6; i++) {
            boolean handled = loadBalancer.distributeRequest("Request " + i);
        }
        System.out.println(" All requests distributed evenly using round-robin algorithm");

        // =========================================================================
        // PHASE 2: AUTOMATIC HEALTH FAILURE DETECTION
        // Demonstrates: Automatic server exclusion when health checks fail
        // Key Feature: No manual intervention required - system self-heals
        // Expected: Only Server-1 and Server-3 handle requests
        // =========================================================================
        System.out.println("\n=== Phase 2: Health Check Failure ===");
        System.out.println(" Simulating server health failure (Server-2 becomes unresponsive)...");

        // Simulate Server-2 becoming unhealthy (network issues, app crash, etc.)
        server2.setHealthy(false);
        Thread.sleep(500); // Allow health check system to detect the failure

        System.out.println(" Distributing requests after health failure detection:");
        for (int i = 7; i <= 12; i++) {
            boolean handled = loadBalancer.distributeRequest("Request " + i);
        }
        System.out.println(" Load balancer automatically excluded unhealthy Server-2 from rotation");

        // Phase 3: Health recovery
        System.out.println("\n=== Phase 3: Health Recovery ===");
        System.out.println("Server-2 recovering from health failure...");
        server2.setHealthy(true);
        Thread.sleep(500); // Allow health check recovery detection

        System.out.println("Distributing requests after recovery:");
        for (int i = 13; i <= 18; i++) {
            boolean handled = loadBalancer.distributeRequest("Request " + i);
        }

        // =========================================================================
        // PHASE 4: DUAL STATE MANAGEMENT (Most Important Concept!)
        // Demonstrates: Administrative control vs automatic health monitoring
        // Key Innovation: Two separate but complementary state management systems
        // Business Value: Operational control + automatic failure detection
        // =========================================================================
        System.out.println("\n=== Phase 4: Administrative vs Health Control ===");
        System.out.println(" Demonstrating: Why we separate manual and automatic server management");

        System.out.println("\n Administrative Action: Taking Server-1 out for planned maintenance");
        server1.setAvailable(false); // Manual/administrative control (planned)
        server1.setHealthy(true);     // Still healthy from health check perspective

        System.out.println(" Health Failure: Server-3 stops responding to health checks");
        server3.setAvailable(true);  // Still administratively available
        server3.setHealthy(false);   // But health monitoring detected failure

        Thread.sleep(500); // Allow health check system to process changes

        System.out.println("\n Result: Only Server-2 can serve requests (available AND healthy)");
        System.out.println("Server-1: Unavailable (admin) + Healthy (health) = No requests");
        System.out.println("Server-2: Available (admin) + Healthy (health) =  Serves requests");
        System.out.println("Server-3: Available (admin) + Unhealthy (health) = No requests");

        for (int i = 19; i <= 22; i++) {
            boolean handled = loadBalancer.distributeRequest("Request " + i);
        }
        System.out.println(" Dual state management provides operational clarity and control");

        // Phase 5: Strategy switching with health checking
        System.out.println("\n=== Phase 5: Strategy Switching ===");
        System.out.println("Restoring all servers and switching to Random strategy...");
        server1.setAvailable(true);
        server1.setHealthy(true);
        server3.setHealthy(true);
        Thread.sleep(500);

        loadBalancer.setLoadBalancingStrategy(new RandomStrategy(42)); // Fixed seed for reproducible results

        for (int i = 23; i <= 28; i++) {
            boolean handled = loadBalancer.distributeRequest("Request " + i);
        }

        // Phase 6: All servers unhealthy
        System.out.println("\n=== Phase 6: Complete System Failure ===");
        System.out.println("All servers failing health checks...");
        server1.setHealthy(false);
        server2.setHealthy(false);
        server3.setHealthy(false);
        Thread.sleep(500);

        System.out.println("Attempting requests with all servers unhealthy:");
        for (int i = 29; i <= 31; i++) {
            boolean handled = loadBalancer.distributeRequest("Request " + i);
        }

        // Cleanup
        System.out.println("\n=== Shutting Down Load Balancer ===");
        loadBalancer.shutdown();
        System.out.println("Health checking stopped. Demo complete!");
    }
}