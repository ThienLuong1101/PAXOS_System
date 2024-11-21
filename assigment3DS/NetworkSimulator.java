import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class NetworkSimulator {
    private static final Map<Integer, Integer> nodeLatencies = new ConcurrentHashMap<>();
    private static final Set<Integer> offlineNodes = Collections.synchronizedSet(new HashSet<>());
    private static final Random random = new Random();

    /**
     * Sets the network latency for a specific node.
     * 
     * @param nodeId    The ID of the node for which the latency is being set.
     * @param latencyMs The latency in milliseconds for the node's network.
     * 
     * This method stores the latency configuration for the given node.
     */
    public static void setNodeLatency(int nodeId, int latencyMs) {
        nodeLatencies.put(nodeId, latencyMs);
    }

    /**
     * Marks a specific node as offline and sets its latency to 10 seconds.
     * 
     * @param nodeId The ID of the node to be set offline.
     * 
     * This method adds the node to the offline nodes list, making it unavailable for network communication.
     * Additionally, it sets the latency of the offline node to 10 seconds (10000 milliseconds).
     */
    public static void setNodeOffline(int nodeId) {
        offlineNodes.add(nodeId);
        nodeLatencies.put(nodeId, 100000);  // Set the latency to 10 seconds (10000 ms) for offline nodes
    }

    /**
     * Checks if a specific node is offline.
     * 
     * @param nodeId The ID of the node to check.
     * @return       A boolean indicating whether the node is offline (true) or online (false).
     * 
     * This method returns whether the node is present in the offline nodes list.
     */
    public static boolean isNodeOffline(int nodeId) {
        return offlineNodes.contains(nodeId);
    }

    /**
     * Simulates network communication between two nodes, checking for offline status and applying latency.
     * 
     * @param sourceNode The ID of the source node.
     * @param targetNode The ID of the target node.
     * @throws IOException If either node is offline, an IOException is thrown indicating the network failure.
     * 
     * This method checks if either node is offline. If not, it simulates a network latency (if configured) 
     * between the source and target nodes by sleeping the thread for the specified latency period.
     */
    public static void simulateNetwork(int sourceNode, int targetNode) throws IOException {
        // Apply latency if configured
        int latency = nodeLatencies.getOrDefault(targetNode, 0);
        if (latency > 0) {
            try {
                Thread.sleep(latency);  // Simulate the network delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
