import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

public class Main {
    /**
     * The main entry point for the Paxos simulation program.
     * 
     * @param args Command-line arguments, where the first argument is the test case number to execute.
     * @throws IOException If there is an error during node communication or initialization.
     * @throws InterruptedException If the program is interrupted during execution.
     * 
     * This method initializes the Paxos nodes, sets up their peers, starts the nodes,
     * and executes the specified test case based on the input argument. It also includes
     * a timer to terminate the program after 20 seconds.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        // Validate input arguments
        if (args.length != 1) {
            System.out.println("Usage: java Main <test_case_number>");
            return;
        }

        int testCase;
        try {
            testCase = Integer.parseInt(args[0]); // Parse test case number
        } catch (NumberFormatException e) {
            System.out.println("Error: Test case number must be an integer.");
            return;
        }

        // Set up peer nodes with corresponding IP addresses and ports
        List<InetSocketAddress> peers = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            peers.add(new InetSocketAddress("localhost", 8000 + i)); // Assign ports 8001 to 8009
        }

        // Run the selected test case based on input
        switch (testCase) {
            case 1:
                runTestCase1(peers); 
                break;
                case 2:
                runTestCase2(peers); 
                break;
            case 3:
                runTestCase3(peers); 
                break;
            case 4:
                runTestCase4(peers); 
                break;
            case 5:
                runTestCase5(peers); 
                break;
            case 6:
                runTestCase6(peers); 
                break;
            case 7:
                runTestCase7(peers);
                break;
            case 8:
                runTestCase8(peers); 
                break;
            case 9:
                runTestCase9(peers);
                break;
            case 10:
                runTestCase10(peers);
                break;
            default:
                System.out.println("Please select a test case from 1 to 10.");
        }

        //terminate the program after 20 sec
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                System.exit(0); // Graceful termination
            }
        }, 20000); 
    }

    // Paxos implementation works in the case where all M1-M9 have immediate responses to voting queries
    private static void runTestCase1(List<InetSocketAddress> peers) {
        Set<Integer> proposerIds = new HashSet<>(Arrays.asList(1)); // Node 1 is proposer

        // Initialize and start Paxos nodes
        List<PaxosNode> nodes = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            PaxosNode node = new PaxosNode(i, peers, proposerIds);
            try {
                node.start(); 
            } catch (IOException e) {
                e.printStackTrace();
            }
            nodes.add(node);
        }

        System.out.println("Test Case 1: Node 1 proposes leadership with proposal number 40.");
        
        // Simulate Node 1 proposing leadership
        new Thread(() -> {
            nodes.get(0).proposeLeadership(40); 
        }).start();
    }

     //Paxos implementation works when two councillors send voting proposals at the same time
     private static void runTestCase2(List<InetSocketAddress> peers) {
        Set<Integer> proposerIds = new HashSet<>(Arrays.asList(1,2));

        // Initialize and start Paxos nodes
        List<PaxosNode> nodes = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            PaxosNode node = new PaxosNode(i, peers, proposerIds);
            try {
                node.start(); 
            } catch (IOException e) {
                e.printStackTrace();
            }
            nodes.add(node);
        }
        int val1 = 50;
        int val2 = 30;
        System.out.println("Test Case 2: Simulate Node 1 proposing "+ val1 + " and Node 2 proposing " + val2 + " concurrently.");

        new Thread(() -> nodes.get(0).proposeLeadership(val1)).start();
        new Thread(() -> nodes.get(1).proposeLeadership(val2)).start();
    }

    //Test Case 3: Node 1 proposes leadership with proposal number 40, Node 3 has a small delay.
    private static void runTestCase3(List<InetSocketAddress> peers) {
        Set<Integer> proposerIds = new HashSet<>(Arrays.asList(1)); // Node 1 is proposer

        // Initialize and start Paxos nodes
        List<PaxosNode> nodes = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            PaxosNode node = new PaxosNode(i, peers, proposerIds);
            try {
                node.start(); 
            } catch (IOException e) {
                e.printStackTrace();
            }
            nodes.add(node);
        }
        System.out.println("Test Case 3: Node 1 proposes leadership with proposal number 40, Node 3 has a small delay.");
        
        // Set network latencies using the NetworkSimulator
        NetworkSimulator.setNodeLatency(3, 500); 
        
        // Node 1 proposes leadership with proposal number 40
        new Thread(() -> {
            try {
                // Simulate network latency between Node 1 and Node 3
                NetworkSimulator.simulateNetwork(1, 3);
                nodes.get(0).proposeLeadership(40); 
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    //Test Case 4: Node 1 proposes leadership with proposal number 60, Node 9 has a large delay.
    private static void runTestCase4(List<InetSocketAddress> peers) {
        Set<Integer> proposerIds = new HashSet<>(Arrays.asList(1));

        // Initialize and start Paxos nodes
        List<PaxosNode> nodes = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            PaxosNode node = new PaxosNode(i, peers, proposerIds);
            try {
                node.start(); 
            } catch (IOException e) {
                e.printStackTrace();
            }
            nodes.add(node);
        }
        System.out.println("Test Case 4: Node 1 proposes leadership with proposal number 60, Node 9 has a large delay.");
        

        NetworkSimulator.setNodeLatency(9, 3000);  // Node 7 has 3-second latency
        
        new Thread(() -> {
            try {
                NetworkSimulator.simulateNetwork(1, 9);
                nodes.get(0).proposeLeadership(60);  // Node 1 proposes leadership with proposal number 60
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    //Test Case 5: Multiple nodes with varying delays.
    private static void runTestCase5(List<InetSocketAddress> peers) {
        Set<Integer> proposerIds = new HashSet<>(Arrays.asList(1));

        // Initialize and start Paxos nodes
        List<PaxosNode> nodes = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            PaxosNode node = new PaxosNode(i, peers, proposerIds);
            try {
                node.start(); 
            } catch (IOException e) {
                e.printStackTrace();
            }
            nodes.add(node);
        }
        System.out.println("Test Case 5: Multiple nodes with varying delays.");
    
        // Set network latencies using the NetworkSimulator
        NetworkSimulator.setNodeLatency(3, 500);  // Node 3 has 500 ms latency
        NetworkSimulator.setNodeLatency(5, 1500); // Node 5 has 1.5-second latency
        NetworkSimulator.setNodeLatency(7, 3000); // Node 7 has 3-second latency
        NetworkSimulator.setNodeLatency(9, 1000); // Node 9 has 1-second latency
    
        new Thread(() -> {
            try {
                // Simulate network latency between Node 1 and other nodes
                NetworkSimulator.simulateNetwork(1, 3);
                NetworkSimulator.simulateNetwork(1, 5);
                NetworkSimulator.simulateNetwork(1, 7);
                NetworkSimulator.simulateNetwork(1, 9);
                nodes.get(0).proposeLeadership(70);  // Node 1 proposes leadership with proposal number 70
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    //Test Case 6: Node 2 as proposer with latency response.
    private static void runTestCase6(List<InetSocketAddress> peers) {
        System.out.println("Test Case 6: Node 2 as proposer with latency response.");
        Set<Integer> proposerIds = new HashSet<>(Arrays.asList(2));

        // Initialize and start Paxos nodes
        List<PaxosNode> nodes = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            PaxosNode node = new PaxosNode(i, peers, proposerIds);
            try {
                node.start(); 
            } catch (IOException e) {
                e.printStackTrace();
            }
            nodes.add(node);
        }
        NetworkSimulator.setNodeLatency(2, 3000);  
      
        new Thread(() -> {
            try {
                for (int i = 3; i < 9; i++) {
                    NetworkSimulator.simulateNetwork(2, i);
                }
                nodes.get(1).proposeLeadership(80); 
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Test Case 7: Node 2 as proposer with latency propose. (node 8 and 9 receive proposal value later than the other nodes)
    private static void runTestCase7(List<InetSocketAddress> peers) {
        Set<Integer> proposerIds = new HashSet<>(Arrays.asList(2));

        // Initialize and start Paxos nodes
        List<PaxosNode> nodes = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            PaxosNode node = new PaxosNode(i, peers, proposerIds);
            try {
                node.start(); 
            } catch (IOException e) {
                e.printStackTrace();
            }
            nodes.add(node);
        }
        System.out.println("Test Case 7: Node 2 as proposer with latency propose");

        NetworkSimulator.setNodeLatency(8, 3000); 
        NetworkSimulator.setNodeLatency(9, 2000); 
        NetworkSimulator.setNodeLatency(2, 1000);  
        // Start the test case in a new thread
        new Thread(() -> {
            try {
                // Simulate communication between node 2 and other nodes
                for (int i = 3; i < 9; i++) {
                    // Simulate network with specific latencies
                    if (i != 8 && i != 9) {
                        NetworkSimulator.simulateNetwork(2, i);
                    } else {
                        NetworkSimulator.simulateNetwork(2, i);
                    }
                }

                nodes.get(1).proposeLeadership(80);  

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }


    // Test Case 8: Simulate Node 1 proposing and Node 2 proposing concurrently with network latency.
    private static void runTestCase8(List<InetSocketAddress> peers) {
        Set<Integer> proposerIds = new HashSet<>(Arrays.asList(1,2));

        // Initialize and start Paxos nodes
        List<PaxosNode> nodes = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            PaxosNode node = new PaxosNode(i, peers, proposerIds);
            try {
                node.start(); 
            } catch (IOException e) {
                e.printStackTrace();
            }
            nodes.add(node);
        }
        int val1 = 30;
        int val2 = 50;
        System.out.println("Test Case 8: Simulate Node 1 proposing " + val1 + " and Node 2 proposing " + val2 + " concurrently with network latency.");

        // Set specific latencies for each node if necessary
        NetworkSimulator.setNodeLatency(1, 2000);  // Node 1 has a 2-second latency
        NetworkSimulator.setNodeLatency(2, 1000);  // Node 2 has a 1-second latency

        // Start two threads that simulate proposals from both nodes
        new Thread(() -> {
            try {
                // Simulate network delay before Node 1 makes its proposal
                NetworkSimulator.simulateNetwork(1, 2);  // Node 1 communicating with Node 2
                nodes.get(0).proposeLeadership(val1);  // Node 1 proposes leadership value 30
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                // Simulate network delay before Node 2 makes its proposal
                NetworkSimulator.simulateNetwork(2, 1);  // Node 2 communicating with Node 1
                nodes.get(1).proposeLeadership(val2);  // Node 2 proposes leadership value 50
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Test Case 9: Simulate Node 3 proposing a value and then going offline.
    private static void runTestCase9(List<InetSocketAddress> peers) {
        Set<Integer> proposerIds = new HashSet<>(Arrays.asList(3));

        // Initialize and start Paxos nodes
        List<PaxosNode> nodes = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            PaxosNode node = new PaxosNode(i, peers, proposerIds);
            try {
                node.start(); 
            } catch (IOException e) {
                e.printStackTrace();
            }
            nodes.add(node);
        }
        System.out.println("Test Case 9: Node 3 as proposer goes offline.");
    
        // Mark Node 3 as offline
        NetworkSimulator.setNodeOffline(3);
    
        // Start a thread for Node 3's proposal and interaction
        new Thread(() -> {
            try {
                for (int i = 1; i < 9; i++) {
                    if (i == 3) continue;
                    NetworkSimulator.simulateNetwork(3, i);
                }
                nodes.get(2).proposeLeadership(80);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    
        // Schedule program termination after 10 seconds
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Proposer went offline. No leader is chosen");
                System.exit(0); 
            }
        }, 20000);
    }
    
     // Test Case 10: Simulate Node 3 proposing a value and then going offline and going online again.
     private static void runTestCase10(List<InetSocketAddress> peers) {
        Set<Integer> proposerIds = new HashSet<>(Arrays.asList(3));

        // Initialize and start Paxos nodes
        List<PaxosNode> nodes = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            PaxosNode node = new PaxosNode(i, peers, proposerIds);
            try {
                node.start(); 
            } catch (IOException e) {
                e.printStackTrace();
            }
            nodes.add(node);
        }
        System.out.println("Test Case 10: Node 3 as proposer goes offline then go online.");
    
        // Mark Node 3 as offline
        NetworkSimulator.setNodeLatency(2, 5000);
    
        // Start a thread for Node 3's proposal and interaction
        new Thread(() -> {
            try {
                for (int i = 1; i < 9; i++) {
                    NetworkSimulator.simulateNetwork(3, 5);
                    NetworkSimulator.simulateNetwork(3, i);
                }
                nodes.get(2).proposeLeadership(80);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
