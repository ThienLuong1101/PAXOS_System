

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class PaxosNode {
    private final int nodeId;
    private ServerSocket serverSocket;
    private final List<InetSocketAddress> peers;
    private final Set<Integer> proposerIds; // Set of nodes that are proposers
    private boolean isProposer;

    private final Object proposalLock = new Object();
    private int highestProposalValue = -1;
    private int acceptedProposalValue = -1;

    private static final Map<Integer, Integer> proposalCount = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> promiseCount = new ConcurrentHashMap<>();
    private static final Set<Integer> activeProposers = Collections.synchronizedSet(new HashSet<>());

    private final ExecutorService executor = Executors.newFixedThreadPool(3);


    /**
     * Constructor for the PaxosNode class.
     * 
     * @param nodeId The unique identifier for this node.
     * @param peers A list of addresses for peer nodes in the Paxos network.
     * @param proposerIds A set of IDs representing nodes that can act as proposers.
     * 
     * This constructor initializes a PaxosNode with its unique ID, a list of peers,
     * and a set of proposer IDs. It also determines whether this node is a proposer.
     */
    public PaxosNode(int nodeId, List<InetSocketAddress> peers, Set<Integer> proposerIds) {
        this.nodeId = nodeId;
        this.peers = peers;
        this.proposerIds = proposerIds;
        this.isProposer = proposerIds.contains(nodeId);
    }

    /**
     * Starts the Paxos node by setting up a server socket to listen for incoming messages
     * and determining the node's role (Proposer or Acceptor). 
     * 
     * @throws IOException If the server socket cannot be bound to the specified port.
     * 
     * This method initializes the server socket with a port based on the node's ID,
     * logs the node's role, and starts a separate thread to handle incoming connections.
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(8000 + nodeId);
        String role = isProposer ? "PROPOSER" : "ACCEPTOR";
        System.out.println("Node " + nodeId + " started as " + role + ", listening on port " + (8000 + nodeId));

        new Thread(this::listenForMessages).start(); // Start listening for messages
    }

    /**
     * Continuously listens for incoming connections on the server socket. 
     * 
     * This method accepts client connections in a loop, spawns a new thread for each 
     * connection, and processes the received message.
     * 
     * Handles exceptions to ensure the server keeps running even in the case of errors 
     * while accepting or processing a message.
     */
    private void listenForMessages() {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        String message = reader.readLine();
                        if (message != null) {
                            processMessage(message);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Processes incoming messages based on their type and the role of the current node (Proposer or Acceptor).
     * 
     * @param message The received message, formatted as "type:nodeId:value".
     * 
     * This method delegates message handling to appropriate handlers (`handlePromise`, `handleAccept`,
     * `handleProposal`, or `handleLeaderDeclaration`) using an executor for asynchronous processing.
     */
    private void processMessage(String message) {
        String[] parts = message.split(":");

        executor.submit(() -> {
            // If this node is a proposer, it only processes promises and accepts
            if (isProposer) {
                switch (parts[0]) {
                    case "promise":
                        handlePromise(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                        break;
                    case "accept":
                        handleAccept(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                        break;
                }
            } 
            // If this node is an acceptor, it only processes proposals and leader declarations
            else {
                switch (parts[0]) {
                    case "propose":
                        handleProposal(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                        break;
                    case "declareLeader":
                        handleLeaderDeclaration(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                        break;
                }
            }
        });
    }


    /**
     * Handles a proposal received by an Acceptor node.
     * 
     * @param proposerId The ID of the proposer node sending the proposal.
     * @param proposalValue The value of the proposal.
     * 
     * This method evaluates the proposal against the highest proposal value seen so far.
     * If the proposal is acceptable, the Acceptor updates its internal state and sends a
     * promise back to the proposer.
     */
    private void handleProposal(int proposerId, int proposalValue) {
        if (!isProposer) {
            synchronized (proposalLock) {
                synchronizedOutput(() -> {
                    System.out.println("ACCEPTOR Node " + nodeId + " received proposal from PROPOSER Node " + 
                                    proposerId + " with value: " + proposalValue);
                });

                if (proposalValue > highestProposalValue) {
                    highestProposalValue = proposalValue;
                    acceptedProposalValue = proposalValue;
                    synchronizedOutput(() -> {
                        System.out.println("ACCEPTOR Node " + nodeId + " accepts proposal " + 
                                        proposalValue + " from PROPOSER Node " + proposerId);
                    });

                    sendMessage(peers.get(proposerId - 1), "promise:" + nodeId + ":" + proposalValue);
                }
            }
        }
    }

        /**
     * Handles a promise message received by a Proposer node.
     * 
     * @param acceptorId The ID of the Acceptor node sending the promise.
     * @param promisedValue The proposal value for which the promise was made.
     * 
     * This method increments the count of promises received for a specific proposal value.
     * Once the required majority of promises is reached, the Proposer declares itself as the leader.
     */
    private void handlePromise(int acceptorId, int promisedValue) {
        if (isProposer) {
            synchronizedOutput(() -> {
                System.out.println("PROPOSER Node " + nodeId + " received promise from ACCEPTOR Node " + 
                                acceptorId + " with value: " + promisedValue);
            });

            promiseCount.put(promisedValue, promiseCount.getOrDefault(promisedValue, 0) + 1);

            int requiredPromises = (peers.size() - proposerIds.size()) / 2 + 1;
            
            if (promiseCount.get(promisedValue) >= requiredPromises) {
                declareLeader(promisedValue);
            }
        }
    }

    /**
     * Declares the current node as the leader for a specific proposal value.
     * 
     * @param proposalValue The proposal value for which leadership is being declared.
     * 
     * This method ensures thread-safe declaration of leadership using a synchronized block. 
     * If this proposal value has not already been declared as having a leader, it adds the proposal
     * value to the `declaredLeaders` set and sends a "declareLeader" message to all peers.
     */
    private final Set<Integer> declaredLeaders = Collections.synchronizedSet(new HashSet<>()); // Track declared leaders

    private void declareLeader(int proposalValue) {
        synchronized (proposalLock) { // Synchronize to avoid multiple threads entering this block
            synchronizedOutput(() -> {
                if (!declaredLeaders.contains(proposalValue)) {
                    System.out.println("Node " + nodeId + " is declaring itself as the leader for proposal value " + proposalValue);
                    declaredLeaders.add(proposalValue); // Mark this proposal value as having declared a leader
                }
            });

            // Send leadership declaration to peers
            for (InetSocketAddress peer : peers) {
                sendMessage(peer, "declareLeader:" + nodeId + ":" + proposalValue);
            }
        }
    }

    /**
     * Handles an "accept" message sent from a proposer to this acceptor.
     * 
     * @param proposerId    The ID of the proposer sending the message.
     * @param proposalValue The proposal value being accepted.
     * 
     * Acceptors will update their `highestProposalValue` and `acceptedProposalValue` 
     * if the received proposal value is higher. Sends a "promise" message back to the proposer.
     */
    private void handleAccept(int proposerId, int proposalValue) {
        if (!isProposer) {  // Only acceptors will handle the accept message
            synchronized (proposalLock) {
                if (proposalValue > highestProposalValue) {
                    highestProposalValue = proposalValue;
                    acceptedProposalValue = proposalValue;
                    sendMessage(peers.get(proposerId - 1), "promise:" + nodeId + ":" + proposalValue);
                }
            }
        }
    }

    /**
     * Proposes leadership for a given proposal value.
     * 
     * @param proposalValue The value for which leadership is proposed.
     * 
     * Proposers will send a "propose" message to all acceptors, asking them to consider 
     * the proposal value. The method ensures thread safety while sending the proposal.
     */
    public void proposeLeadership(int proposalValue) {
        if (isProposer) {  // Only proposers can propose leadership
            synchronized (proposalLock) {
                synchronizedOutput(() -> {
                    System.out.println("PROPOSER Node " + nodeId + " is proposing leadership for value " + proposalValue);
                });
                // Send the proposal to the acceptors
                for (InetSocketAddress peer : peers) {
                    int peerId = peer.getPort() - 8000;
                    if (!proposerIds.contains(peerId)) {
                        sendMessage(peer, "propose:" + nodeId + ":" + proposalValue);
                    }
                }
            }
        }
    }


    /**
     * Handles a "leader declaration" message received from a proposer.
     * 
     * @param leaderId    The ID of the node declaring itself as the leader.
     * @param leaderValue The proposal value associated with the leader.
     * 
     * The method ensures that a leader declaration is only agreed upon if the proposal value 
     * is higher than or equal to this node's current highest proposal value. Updates agreement 
     * state and prints whether the node agrees or disagrees.
     */
    private final Set<Integer> agreedProposals = Collections.synchronizedSet(new HashSet<>()); // Track agreed proposals
    private void handleLeaderDeclaration(int leaderId, int leaderValue) {
        synchronizedOutput(() -> {
            if (!agreedProposals.contains(leaderValue)) {
                if (leaderValue >= highestProposalValue) {
                    System.out.println("Node " + nodeId + " agrees with leader declaration: Node " + leaderId + " is the leader.");
                    highestProposalValue = leaderValue;  // Update the highest proposal value
                    agreedProposals.add(leaderValue); // Mark this proposal value as agreed
                } else {
                    System.out.println("Node " + nodeId + " disagrees with leader declaration: Node " + leaderId + " is not the leader.");
                }
            }
        });
    }


    /**
     * Sends a message to a target node via its address.
     * 
     * @param targetAddress The address of the target node.
     * @param message       The message to be sent.
     * 
     * Simulates network latency using `NetworkSimulator`, establishes a socket connection 
     * to the target node, and sends the message. Ensures proper handling of exceptions 
     * during communication.
     */
    private void sendMessage(InetSocketAddress targetAddress, String message) {
        try {
            // Simulate network latency for the target node
            int targetNodeId = targetAddress.getPort() - 8000;
            NetworkSimulator.simulateNetwork(this.nodeId, targetNodeId);

            Socket clientSocket = new Socket(targetAddress.getHostName(), targetAddress.getPort());
            try {
                PrintWriter socketWriter = new PrintWriter(clientSocket.getOutputStream(), true);
                socketWriter.println(message);
            } catch (Throwable connectionError) {
                try {
                    clientSocket.close();
                } catch (Throwable closeError) {
                    connectionError.addSuppressed(closeError);
                }
                throw connectionError;
            }
            clientSocket.close();
        } catch (IOException communicationError) {
            communicationError.printStackTrace();
        }
    }

    
    /**
     * Executes a given action within a synchronized block for standard output.
     * 
     * @param action The action to be executed.
     * 
     * Ensures that output to the console is thread-safe by synchronizing access 
     * to the `System.out` object.
     */
    private void synchronizedOutput(Runnable action) {
        synchronized (System.out) {
            action.run();
        }
    }

    
}
