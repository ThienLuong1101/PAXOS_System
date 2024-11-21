### HOW TO RUN
javac *.java
java Main.java n //where n is the number of test case from 1 to 10
Ex: java Main.java 1 //run test case 1

## Note: 
Propose values are unique
Program automatically terminates after 15 seconds

### Description:

Paxos Distributed Consensus System
This system implements a simplified version of the Paxos consensus algorithm, designed for distributed nodes to 
agree on a leader in a fault-tolerant manner. The system allows for nodes to propose leadership, vote on proposals, and 
declare a leader once consensus is reached. It simulates the behavior of both proposers and acceptors in the Paxos protocol.

# Basic Flow
Proposer nodes attempt to become the leader by proposing a value
Acceptor nodes promise to accept the highest proposal they've seen
When a proposer receives promises from a majority of acceptors, it declares itself as leader
The system ensures that only one leader is elected for each valid proposal

# Features
Proposer Role: A proposer node can propose a leadership value and send proposals to acceptor nodes.
Acceptor Role: An acceptor node listens for proposals and promises to accept a proposal if it is higher 
than any previously accepted value.
Leader Declaration: Once a majority of acceptors promise to accept a proposal, the proposer node declares the leader. 
Acceptors listen for leader declarations and agree to the first valid leader they receive.

# Components
PaxosNode: Each node in the system can function either as a proposer or an acceptor.

Proposers: propose leadership values.

Acceptors promise to accept proposals and can later declare a leader once enough promises have been received.

Leader Election: Once a proposer receives enough promises, it declares itself as the leader and broadcasts the leader 
declaration to all nodes. Acceptors then agree with the leader or reject the declaration if the proposed value is not valid.

Timeout Handling: If no leader is declared after a certain period (e.g., 5 seconds), the proposal value is removed, 
and the system attempts another leader election.

# Setup
Create Nodes: Initialize each node with a unique nodeId and a list of peers' network addresses. 
Nodes can be either proposers or acceptors.

Start Nodes: Each node listens for incoming messages on a port corresponding to its nodeId.

Propose Leadership: The proposer node sends out proposals to the acceptors to request their support.

Promise Handling: Acceptors respond with promises if the proposal value is higher than any previously seen proposal.

Leader Declaration: Once enough promises are received, the proposer declares a leader.


### Test Cases

Test Case 1: Node 1 Proposes Leadership (Proposal 40)
Description: Node 1 proposes leadership with proposal number 40.
Expected Outcome: The Paxos nodes communicate and vote on the proposal. 
Since there are no delays, the proposal should succeed.

Test Case 2: Concurrent Proposals by Node 1 and Node 2
Description: Node 1 proposes a leadership value of 30, while Node 2 proposes a leadership value of 50 concurrently.
Expected Outcome: Both proposals are made simultaneously. The Paxos protocol should resolve the conflict, 
and one of the proposals will be accepted.

Test Case 3: Proposal by Node 1 with Network Delay to Node 3
Description: Node 1 proposes leadership with proposal number 40, but Node 3 has a small network delay of 500ms.
Expected Outcome: The proposal should be delayed for Node 3 due to latency,
 but the Paxos protocol should eventually reach a consensus.

Test Case 4: Proposal by Node 1 with Large Delay to Node 9
Description: Node 1 proposes leadership with proposal number 60, 
while Node 9 experiences a large network delay of 3 seconds.
Expected Outcome: The proposal will be delayed for Node 9 but should be handled by the Paxos protocol once Node 9 responds.

Test Case 5: Multiple Nodes with Varying Delays
Description: Node 1 proposes leadership with proposal number 70, while other nodes (Node 3, 5, 7, 9) 
have varying network latencies (500ms, 1.5s, 3s, 1s respectively).
Expected Outcome: The proposal will be delayed for the nodes with higher latencies,
 but the Paxos protocol should eventually achieve consensus.

Test Case 6: Node 2 as Proposer with Network Delay
Description: Node 2 proposes leadership with proposal number 80, 
while it experiences a 3-second delay when communicating with other nodes.
Expected Outcome: The proposal will experience delays but will be handled by the Paxos protocol, 
eventually achieving consensus.

Test Case 7: Node 2 as Proposer with Latency to Node 8 and Node 9
Description: Node 2 proposes leadership with value 80, but Nodes 8 and 9 experience
higher latencies compared to other nodes.
Expected Outcome: Nodes 8 and 9 will receive the proposal later, but the Paxos protocol will 
allow a consensus even with delayed responses.

Test Case 8: Concurrent Proposals by Node 1 and Node 2 with Network Latency
Description: Node 1 proposes leadership with value 30 and Node 2 proposes leadership with value 50 concurrently, 
while both nodes experience different network latencies.
Expected Outcome: The concurrent proposals will be handled by Paxos, 
resolving the conflict and eventually selecting one proposal.

Test Case 9: Node 3 Proposes Leadership and Goes Offline
Description: Node 3 proposes a leadership value, but then goes offline.
The program should handle the situation where no leader is chosen.
Expected Outcome: As Node 3 is offline, no leader is chosen. The program will exit after 20 seconds, indicating no leader.

Test Case 10: Node 3 Proposes Leadership, Goes Offline, and Then Comes Back Online
Description: Node 3 proposes leadership, goes offline, and then comes back online. 
The program simulates the scenario where a proposer goes offline and re-joins the network.
Expected Outcome: The proposal process should continue after Node 3 comes back online, and the Paxos protocol 
should proceed to achieve consensus.