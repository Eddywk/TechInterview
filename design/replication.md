# Replication
* **High availability**
  * Keeping the system running even when one machine goes down.
* **Disconnected operation**
  * Allowing an application to continue working when there is a network interruption.
* **Latency**
  * Placing data geographically close to users, so that users can interact with it faster.
* **Scalability**
  * Being able handling a higher volume of reads than a single machine could happen, by performing reads on replicas.

## Glossary

* **_Replica_**: Each node that stores a copy of the database is called a replica.
* **_Leader-based replication_**: Clients' write requests go to leader node and then be processed to follower nodes.

![Leader-based replication](https://ebrary.net/htm/img/15/554/41.png)

* **_semi-synchronous_**: Having one follower is synchronous all the time (will be replaced by another follower when the
synchronous follower is unavailable or slow) that guarantees you have at least an up-to-date copy of the data on 
at least two nodes: the leader and one synchronous follower.
* **_Failover_**: The process to handle leader node failure.
  1. Determining that the leader has failed.
     1. No foolproof way, most systems simply setup timeout for leader-client connection to decide if a leader has 
  failed.
  2. Choosing a new leader.
     1. By leader election process: the leader is chosen by a majority of the remaining replicas.
     2. By appointment: A new leader could be appointed by a previously elected _controller node_.
  3. Reconfiguring the system to use the new leader.
     1. The system needs to ensure that the old leader becomes a follower and recognizes the new leader.
* **_Split brain_**: Two nodes both believe that they are the leader.
* **_Replication logs_**: The logs are used to make followers keep in sync with leader.
  * **Statement-based replication**: Forward INSERT, UPDATE or DELETE statement to followers. Side effects: statement order 
matters and some functions like NOW() and RAND() are nondeterministic on followers.
  * **Write-ahead log(WAL) shipping**: Ship WAL to followers. This low-level approach couples your replication with 
storage engine. 
  * **Logical (row-based) log replication**: the logs contains logical row information. For INSERT/UPDATE, 
the log contains the new values of all columns; For DELETE, the log contains enough information to uniquely identify the
row that was deleted.

## Problems with Replication Lag

### Reading Your Own Writes
**Problem**: A user makes a write, followed by a read from a stale replica.
![A user makes a write, followed by a read from a stale replica](https://ebrary.net/htm/img/15/554/43.png)

**Solutions**:
Read-after-write consistency (aka read-your-writes consistency) is a guarantee that if the user reloads the page, they
will always see any updates they submitted themselves.
* Reading from leader: when reading something that the user may have modified from leader otherwise from followers.
* Setup other criteria: to decide weather to read from leader. For example, all reads of last minute go to leader. Or
monitoring the replication lag on followers and prevent queries on any follower that more than one minute behind the 
leader.
* Client timestamp: The system can ensure that the replica serving any reads for that user not behind the user's
last-seen timestamp.

### Monotonic Reads
**Problem**: A user first reads from a fresh replica, then from a stale replica.
![A user first reads from a fresh replica, then from a stale replica.](https://ebrary.net/htm/img/15/554/44.png)

**Solution**:
Monotonic Reads is a guarantee that this kind of anomaly does not happen.
* Using replica partitioning to ensure each user always make their reads from the same replica rather than randomly.

### Consistent Prefix Reads
**Problem**: If some partitions are replicated slower than others, an observer may see the answer before they see the 
question.
![If some partitions are replicated slower than others, an observer may see the answer before they see the question.](https://ebrary.net/htm/img/15/554/45.png)

**Solution**:
Consistent prefix reads is a guarantee that if a sequence of writes happens in a certain order, then anyone reading
those writes will see them appear in the same order.
* One solution is to make sure that any writes that causally related to each other are written to the same partition. 

## Multi-Leader Replication

### Use Cases
* Multi-datacenter operation
  * You can have a leader in each datacenter
  * Within each datacenter, regular leader-based replication is used
  * Between datacenters, each datacenter's leader replicates its changes to the leaders in other datacenters
* Clients with offline operation
  * Every device has a local database that acts a leader.
* Collaborative editing
  * Google Docs

### Handling Write Conflicts
* Conflict avoidance
  * Using partitioning to always route a user's write requests to the same datacenter.
* Converging toward a consistent state
  * There are various ways of achieving this: Give each write a unique ID, if a timestamp is used this technique is 
known as **last write win (LWW)**.
* Custom conflicts resolution logic

### Multi-Leader Replication Topologies
A **_replication topology_** describes the communication paths along which writes are propagated from one node to another.
![Three example topologies in which multi-leader replication can be set up](https://ebrary.net/htm/img/15/554/48.png)

The most general topology is _all-to-all_. but it also has happens-before problem (Consistent Prefix Reads).

The problem of _circular_ and _star_ topologies is one node fails, it can interrupt the flow of replication messages between
other nodes.

## Leaderless Replication
In a leaderless configuration, read and write requests are not only sent to one replica, they are sent to several nodes
in parallel. Version numbers are used to determine which value is newer. Failover does not exist in leaderless 
replication. 

![A quorum write, quorum read, and read repair after a node outage](https://ebrary.net/htm/img/15/554/50.png)

In a **_n_** replicas leaderless configuration, every write must be confirmed by **_w_** nodes to be considered 
successful, and we must query at least **_r_** nodes for each read to ensure we get up-to-date data. The **_w + r > n_**
we use here is called **_quorum_**. 

Normally reads and writes are always sent to all n replicas in parallel, the 
parameter w and r determine how many nodes we wait for.

Often, r and w are chosen to be a majority (more than n/2) of nodes, because that ensures w + r > n while still 
tolerating up to n/2 node failures. 

**Read repair**: When a read request gets multiple versions, the client writes the newer value back to replicas that
hold the old value. This works well for frequent read values but not for rarely read ones.

**Anti-entropy process**: A background process that constantly looks for differences in the data between replicas and 
copies any missing data from one replica to another.

**Sloppy quorum**: Writes and reads still require w and r successful responses, but those may include nodes that are not 
among the designated n "home" nodes for a values. Sloppy quorum are particularly useful for increasing write 
availability: as long as any w nodes available.

**Hinted handoff**: Once the network interruption is fixed, any writes that one node temporarily accepted on behalf of 
another node are sent to the appropriate "home" nodes. This is called hinted handoff. There is no guarantee client can
read up-to-date value until hinted handoff completed.

### Detect Concurrent Writes
Whether one operation happens before another operation is the key to define what concurrency means. In fact, we can
simply say that two operations are concurrent if neither happens before the other (without knowing about each other).

So, a server can determine whether two operations are concurrent by looking at the version numbers.

In the following diagram, server can accept or reject a write request by comparing version numbers.

![Capturing causal dependencies between two clients concurrently editing a shopping cart.](https://ebrary.net/htm/img/15/554/54.png)

When a write includes the version from a prior read, that tells us which previous state the write is based on.

If we use version number in multi-replica case, the collection of version numbers from all the replicas is called 
**_version vector_**.