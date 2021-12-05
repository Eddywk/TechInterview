# Partitioning

The main reason of partitioning data is **_scalability_**.

The goal of partitioning is to spread data and query load evenly across nodes.

## Glossary
* **_Partitions_**: For very large datasets, or very high query throughput, we need to break the data up into 
partitions. Partition is also called **_shard_**, **_vnode_**, **_region_**, **_tablet_** and **_vBucket_** in different 
systems.
* **_Skewed_**: Some partitions have more data or queries than others.
* **_Hot spot_**: A partition with disproportionately high load.
* **_Consistent hashing_**: A way of evenly distributing load across an internet-wide system of caches such as a CDN.
* **_Compound primary key_**: Only the first part of that key is hashed to determine the partition, but the other 
columns are used as concatenated index for sorting the data.
* **_Secondary index_**: A secondary index usually doesn't identify a record uniquely but rather than is a way of 
searching for occurrences of a particular value. e.g. color: red
* **_Local index_**: Document-partitioned index for secondary indexes.
* **_Scatter/gather_**: Querying a partitioned database. For example, make read query on a secondary index column on all 
local indexes. Scatter/gather is prone to tail latency amplification.
* **_Global index_**: Term-partitioned index for secondary indexes.
* **_Rebalancing_**: The process of moving load from one node in the cluster to another.
* **_Service discovery_**: Routing client requests to the right node.

## Partitioning and Replication
Combining replication and partitioning: each node acts as leader for some partitions and follower for other partitions.

The copies of each partition are stored on multiple nodes for fault tolerance.
![Combining replication and partitioning](https://ebrary.net/htm/img/15/554/58.png)

## Key-Value Data Partitioning

### Partitioning by Key Range

![A print encyclopedia is partitioned by key range](https://ebrary.net/htm/img/15/554/59.png)

**Pros**:
* **Range scans are easy**
  * With in each partition, we can keep keys in sorted order. 
(see [SSTable and LSM-Tree](https://github.com/Eddywk/TechInterview/blob/main/design/database.md#sstables-and-lsm-trees))
  * You can treat the key a concatenated index in order to fetch several related records in one query.

**Cons**:
* **The range of keys are not necessarily evenly spaced**
  * Because your data may not be evenly distributed.
* **Certain access patterns can lead to hot spots**
  * You need to carefully design and choose the right key to avoid this problem.
  * Choose an evenly distributed column as the prefix of your key. For example, userId_timestamp. 

### Partitioning by Hash of Key

![Partitioning by hash of key](https://ebrary.net/htm/img/15/554/60.png)

**Pros**:
* **Data is uniformly distributed**
  * Once you have a suitable hash function, for example, String -> Hash Function -> An integer ranges in 2^32 -1. 
Then you can assign each partition a range of hashes, and evert key whose hash falls within a partition's range will be
stored in that partition.

**Cons**:
* **Inability to perform efficient range queries**
  * Reads for range query have to go to all partitions.

## Secondary Index Partitioning

### Partitioning Secondary Indexes by Document
In this indexing approach,

* **Each partition is completely separate**
  * Each partition maintains its own secondary index, covering only the documents in that partition.
  * Whenever you write to the database: add/remove/update a document, you only need to deal with the partition that
the documentId belongs to.
* **Scatter/gather might be needed**
  * Sometimes, secondary index queries cannot be served from a single partition.
For example, you filter cars by color:red and make:honda at the same time.

![Partitioning secondary indexes by document](https://ebrary.net/htm/img/15/554/61.png)

### Partitioning Secondary Indexes by Term
**Pros**:
* **Secondary index queries are more efficient**
  * No need to do scatter/gather, a client only needs to make a request to the partition containing the term it wants.

**Cons**:
* **Writes are slower and more complicated**
  * A write may affect multiple partitions of the index, every term in a document might be on a different partition on 
a different node.
  * Updates to global indexes are often asynchronous and may experience longer propagation delays.

![Partitioning secondary indexes by term](https://ebrary.net/htm/img/15/554/62.png)

## Rebalancing

**Requirements**:
* The load should be shared fairly between the nodes in the cluster after rebalancing
* The database should continue accepting reads and writes while rebalancing is happening
* Only necessary data needs to be moved to minimize rebalancing time and network/IO load

### Fixed number of partitions
**Solution**: Create many more partitions than actual nodes.

![Adding a new node to a database cluster with multiple partitions per node](https://ebrary.net/htm/img/15/554/63.png)

* If a node is added to the cluster, the new node can steal a few partitions from every existing node until 
rebalancing is done.
* The old assignment of partitions is used for any reads and writes that happen while the rebalancing is in progress.
* You need to choose high enough number of partitions to accommodate future growth. However, higher number means higher
management overhead.

### Dynamic Partitioning
**Solution**: When a partition grows to exceed a configured size, it is split into two partitions (approximately 
half/half). Conversely, if a lot of data is deleted and a partition shrinks below some threshold, it can be merged with 
an adjacent partition.

Advantages (in comparing with rebalancing of fixed number of partitions):
* Work better for database uses key range partitioning
* The number of partitions adapts to the total data volume

## Request Routing

![Three different ways of routing a request to the right node](https://ebrary.net/htm/img/15/554/64.png)

1. Allow clients to contact any node
   1. The node directly handle the request if it owns the responding partition.
   2. The node forwards the request to the appropriate node, receives the reply and pass the reply to the client.
2. Send all requests from clients to a routing tier first
   1. The load balancer.
3. Require that clients be aware of the partitioning and the assignment of partitions to nodes
   1. The client stores partition information itself.

![Using ZooKeeper to keep track of assignment of partitions to nodes](https://ebrary.net/htm/img/15/554/65.png)

Many distributed data systems rely on a separate coordination service such as ZooKeeper to keep track of this cluster 
metadata. Whenever a partition changes ownership, or a node is added or removed, ZooKeeper notifies the routing tier so 
that it can keep its routing information up to date.