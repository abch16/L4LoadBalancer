package com.testorg.server;

/**
 * Server - Backend Server Implementation
 *
 * 🎯 Purpose: Represents individual backend servers in the 1999 scale-up architecture
 *
 * This class implements the crucial concept of DUAL STATE MANAGEMENT:
 *
 * 🔧 Administrative Availability (isAvailable):
 * - Manual control by operations team
 * - Used for planned maintenance, deployments, capacity management
 * - Set via setAvailable(true/false)
 * - Example: "Take Server-2 out of rotation for OS patching"
 *
 * 💓 Health Status (isHealthy):
 * - Automatic monitoring by HealthCheckManager
 * - Reflects actual server responsiveness and functionality
 * - Updated by health check system every 5 seconds
 * - Example: "Server-3 stopped responding to health checks"
 *
 * 🎯 Key Rule: A server must be BOTH available AND healthy to receive requests
 *
 * 💡 Why Separate These States?
 * - Administrative Control: Ops can manually manage servers
 * - Automatic Detection: System can detect failures without human intervention
 * - Clear Responsibility: Manual vs automatic state management
 * - Operational Clarity: "Is it down by design or by accident?"
 *
 * 🚀 Request Handling Logic:
 * - Available + Healthy = Handle requests normally
 * - Unavailable + Healthy = "Server is administratively down"
 * - Available + Unhealthy = "Server failed health check"
 * - Unavailable + Unhealthy = Still shows administrative reason (takes precedence)
 */
public class Server implements BackendServer {
    private String name;
    private boolean isAvailable;    // Administrative state (manual control)
    private boolean isHealthy;      // Health state (automatic monitoring)

    /**
     * Constructor - Initialize server with healthy defaults
     *
     * New servers start as:
     * - Available: Ready to receive traffic (administrative decision)
     * - Healthy: Assumed healthy until first health check proves otherwise
     */
    public Server(String name) {
        this.name = name;
        this.isAvailable = true;    // Start available (ready for traffic)
        this.isHealthy = true;      // Start healthy (optimistic assumption)
    }

    public String getName() {
        return name;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    @Override
    public boolean isHealthy() {
        return isHealthy;
    }

    @Override
    public void setHealthy(boolean isHealthy) {
        this.isHealthy = isHealthy;
    }

    /**
     * Handle Request - Core server functionality with dual state logic
     *
     * This method demonstrates the practical application of dual state management.
     * It shows how administrative and health states work together to provide
     * clear operational feedback.
     *
     *  Business Logic:
     * - Only handle requests if BOTH available AND healthy
     * - Provide specific error messages for different failure modes
     * - Administrative failures take precedence in error messaging
     *
     *  Why This Matters:
     * - Operations team gets clear feedback on WHY a server isn't serving
     * - Different failure modes may require different remediation actions
     * - Helps distinguish between planned vs unplanned service interruptions
     *
     * Thread Safety Note:
     * - This method is called concurrently by multiple threads
     * - Reading server state (isAvailable, isHealthy) during concurrent updates
     * - May see inconsistent state during transitions
     */
    public boolean handleRequest(String request) {
        // Check both states: must be available AND healthy to serve requests
        if (isAvailable && isHealthy) {
            // Success case: Server can handle the request

            System.out.println("Request \"" + request + "\" handled by server: " + name);
            return true;
        } else {
            // Failure case: Determine WHY server can't handle request
            // Administrative state takes precedence for clearer operational messaging
            String reason = !isAvailable ? "is administratively down" : "failed health check";
            System.out.println("Server " + name + " " + reason + "!");
            return false;
        }
    }

}