## Glossary

* **_Eventual consistency_**: You stop writing to the database and wait for some unspecified length of time, then 
eventually all read requests will return the same value.
* **_Register_**: A single individual object in distributed system terminology.
  * It could be one key in a key-value store, or one row in a relational database, or one document in a document 
database.
* **_Linearizability_**: Linearizability is a recency guarantee on reads and writes of a **_register_**.
  * Linearizability essentially means "behave as though there is only a single copy of the data, 
and all operations on it are atomic."
  * Linearizability is also called **_atomic consistency_**, **_strong consistency_**, **_immediate consistency_** or
**_external consistency_**.
* **_CAP_**: **_Consistency_**, **_Availability_**, **_Partition tolerance_**. Pick 2 out of 3.
  * At times when the network is working correctly, a system can provide both **_consistency (linearizability)_** 
and **_total availability_**.
  * When a network fault occurs, you have to choose between either linearizability or total availability.
  * CAP would be either **_Consistent_** or **_Available when Partitioned_**.
* **_Causality_**: Causality imposes an ordering on events: cause comes before effect; 
a message is sent before that message is received; the question comes before the answer.
* **_Consistent with causality_**: If a system obeys the ordering imposed by causality, we say that it is causally consistent.
* **_Total order_**: A total order allows any two elements to be compared, so if you have two elements, 
you can always say which one is greater and which one is smaller.
  * In a **linearizable system**, we have a total order of operations.
* **_Partial order_**: Some operations are ordered with respect to each other, but some are incomparable.
  * **Causality** defines a partial order, not a total order. two events are ordered if they are causally related 
(one happened before the other), but they are incomparable if they are concurrent.
* **_XA (short for extended Architecture)_**: is a standard for implementing two- phase commit across 
heterogeneous technologies.

## Linearizable System

### What Makes a System Linearizable

1. Recency guarantee: Once a new value has been written or read, all subsequent reads see the value that was written, 
until it is overwritten again.

![](https://ebrary.net/htm/img/15/554/88.png)
After any one read has returned the new value, all following reads (on the same or other clients) must also return 
the new value.

2. Sequential order: operations are executed in a sequential order, and the result must be a valid sequence of reads 
and writes for a register.
   1. Every read must return the value set by the most recent write.

![](https://ebrary.net/htm/img/15/554/89.png)
Visualizing the points in time at which the reads and writes appear to have taken effect. 
The final read by B is not linearizable.

### Linearizability Use Cases

#### Locking and leader election
Apache ZooKeeper implemented distributed locks and leader election. They use consensus algorithms to implement 
linearizable operations in a fault-tolerant way.

#### Constraints and uniqueness guarantees
**Uniqueness constraints** are common in databases: for example, a username or email address must uniquely identify 
one user, and in a file storage service there cannot be two files with the same path and filename.

### Implementing Linearizable Systems
##### Single-leader replication (potentially linearizable)
If you make reads from the leader, or from synchronously updated followers, they have the potential to be linearizable.
However, not every single-leader database is actually line- arizable, either by design 
(e.g., because it uses **_snapshot isolation_**) or due to concurrency bugs.

##### Consensus algorithms (linearizable)
Consensus protocols contain measures to prevent split brain and stale replicas.

##### Leaderless replication (probably not linearizable)

“**_Last write wins_**” conflict resolution methods based on time-of-day clocks are almost certainly nonlinearizable.
because clock timestamps cannot be guaranteed to be consistent with actual event ordering due to clock skew.

**_Sloppy quorums_** also ruin any chance of linearizability

![](https://ebrary.net/htm/img/15/554/91.png)
Even with strict quorums, nonlinearizable behavior is possible

it is possible to make Dynamo-style quorums linearizable at the cost of reduced performance: 
a reader must perform **_read repair_** synchronously, before returning results to the application, 
and a writer must read the latest state of a quorum of nodes before sending its writes.

### Truth about Linearizability
Although linearizability is a useful guarantee, surprisingly few systems are actually linearizable in practice.

The reason for dropping linearizability is performance, not fault tolerance.
Linearizability is slow—and this is true all the time, not only during a network fault.

A faster algorithm for linearizability does not exist, but weaker consistency models can be much faster, 
so this trade-off is important for latency-sensitive systems.

## Ordering Guarantees
In a database with **single-leader replication**, _the replication log defines a **total order** of write operations_ 
that is consistent with causality. The leader can simply increment a counter for each operation, 
and thus assign a monotonically increasing sequence number to each operation in the replication log.


If there is not a single leader, it is less clear how to ensure **consistent with causality**.

### Sequence Number Ordering

#### Lamport timestamps
![](https://ebrary.net/htm/img/15/554/93.png)
Lamport timestamps provide a total ordering consistent with causality.

Lamport timestamp is then simply a pair of (counter, node ID).

### Total Order Broadcast
As discussed, single-leader replication determines a total order of operations by choosing one node as the leader 
and sequencing all operations on a single CPU core on the leader. The challenge then is how to scale the system 
if the throughput is greater than a single leader can handle, and also how to handle **_failover_** if the leader fails.

**_Total order broadcast_** is usually described as a protocol for exchanging messages between nodes.

Total order broadcast requires that two safety properties always be satisfied:
* **Reliable delivery**: No messages are lost: if a message is delivered to one node, it is delivered to all nodes.
* **Totally ordered delivery**: Messages are delivered to every node in the same order.

Total order broadcast is exactly what you need for database replication: 
if every message represents a write to the database, and every replica processes the same writes in the same order, 
then the replicas will remain consistent with each other.

This fact is a hint that there is **a strong connection between total order broadcast and consensus**.
It can be proved that a linearizable compare-and-set (or increment-and-get) register and total order broadcast 
are both **equivalent to consensus**. **Total order broadcast is equivalent to repeated rounds of consensus**.

So, to implement total order broadcast, we need to have a solution for consensus.

## Distributed Transactions and Consensus
The goal of **consensus** is _get several nodes to agree on something_.

With some digging, it turns out that a wide range of problems are actually reducible to consensus and 
are equivalent to each other:

* **Linearizable compare-and-set registers**
  * The register needs to atomically decide whether to set its value, based on whether its current value 
equals the parameter given in the operation.
* **Atomic transaction commit**
  * A database must decide whether to commit or abort a distributed transaction.
* **Total order broadcast**
  * The messaging system must decide on the order in which to deliver messages.
* **Locks and leases**
  * When several clients are racing to grab a lock or lease, the lock decides which one successfully acquired it.
* **Membership/coordination service**
  * Given a failure detector (e.g., timeouts), the system must decide which nodes are alive, and which should be 
considered dead because their sessions timed out.
* **Uniqueness constraint**
  * When several transactions concurrently try to create conflicting records with the same key, 
the constraint must decide which one to allow and which should fail with a constraint violation.

### Two-Phase Commit (2PC)
**_Atomicity_** prevents failed transactions from littering the database with half-finished results and 
half-updated state.

In distributed system, it could easily happen that the commit succeeds on some nodes and fails on other nodes, 
which would violate the atomicity guarantee:
* Some nodes may detect a constraint violation or conflict, making an abort necessary, while other nodes are 
successfully able to commit.
* Some of the commit requests might be lost in the network, eventually aborting due to a timeout, while other 
commit requests get through.
* Some nodes may crash before the commit record is fully written and roll back on recovery,
while others successfully commit.

To ensure atomicity in distributed system, we need **_two-phase commit (2PC)_**.

![](https://ebrary.net/htm/img/15/554/95.png)
A successful execution of two-phase commit (2PC).

The **_coordinator_** is often implemented as a library within the same application process that 
is requesting the transaction.

We call these database nodes **_participants_** in the transaction.

When the application is ready to commit, the coordinator begins phase 1: 
it sends a prepare request to each of the nodes, asking them whether they are able to commit. 
The coordinator then tracks the responses from the participants:
* If all participants reply “yes,” indicating they are ready to commit, then the coordinator 
sends out a commit request in phase 2, and the commit actually takes place.
* If any of the participants replies “no,” the coordinator sends an abort request to all nodes in phase 2.

**_Commit point_**: When the coordinator has received responses to all prepare requests, it makes a definitive decision 
on whether to commit or abort the transaction (committing only if all participants voted “yes”). 
The coordinator must write that decision to its transaction log on disk so that it knows which way it 
decided in case it subsequently crashes.

Once the coordinator’s decision has been written to disk, the commit or abort request is sent to all participants. 
**If this request fails or times out, the coordinator must retry forever until it succeeds.** 
There is no more going back: if the decision was to commit, that decision must be enforced, 
no matter how many retries it takes. If a participant has crashed in the meantime, the transaction will be committed 
when it recovers—since the participant voted “yes,” it cannot refuse to commit when it recovers.

### Fault-Tolerant Consensus
The consensus problem is normally formalized as follows: one or more nodes may propose values, 
and the consensus algorithm decides on one of those values.

A consensus algorithm must satisfy the following properties:
* **Uniform agreement**: No two nodes decide differently. 
* **Integrity**: No node decides twice. 
* **Validity**: If a node decides value v, then v was proposed by some node. 
* **Termination**: Every node that does not crash eventually decides some value.

If you don’t care about **_fault tolerance_**, then satisfying the first three properties is easy: 
you can just hardcode one node to be the “dictator,” and let that node make all of the decisions. 
However, if that one node fails, then the system can no longer make any decisions. 
This is, in fact, what we saw in the case of two-phase commit: if the coordinator fails, 
in-doubt participants cannot decide whether to commit or abort. So, 2PC does not meet the requirements for termination.

#### Epoch numbering and quorums
Every time the current leader is thought to be dead, a vote is started among the nodes to elect a new leader. 
This election is given an incremented **_epoch number_**, and thus epoch numbers are 
**totally ordered and monotonically increasing**.

Before a leader is allowed to decide anything, it must first check that there isn’t some other leader with 
a higher epoch number which might take a conflicting decision.

For every decision that a leader wants to make, it must send the proposed value to the other nodes and wait for 
a **_quorum_** of nodes to respond in favor of the proposal. The quorum typically, but not always, 
consists of a majority of nodes.

Thus, we have two rounds of voting: **once to choose a leader**, and **a second time to vote on a leader’s proposal**. 

The key insight is that the **quorums for those two votes must overlap**: if a vote on a proposal succeeds, 
at least one of the nodes that voted for it must have also participated in the most recent leader election.

Thus, if the vote on a proposal does not reveal any higher-numbered epoch, 
the current leader can conclude that no leader election with a higher epoch number has happened, 
and therefore be sure that it still holds the leadership. It can then safely decide the proposed value.

#### Limitations of consensus
* Consensus systems always require a strict majority to operate.
* Most consensus algorithms assume a fixed set of nodes that participate in voting, which means that 
you can’t just add or remove nodes in the cluster. 
* Consensus systems generally rely on timeouts to detect failed nodes. 
Sometimes, consensus algorithms are particularly sensitive to network problems.