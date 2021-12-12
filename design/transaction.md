# Transactions
* **_Transaction_**: A transaction is a way for an application to group several reads and writes together into a
  logical unit.
  * Conceptually, all the reads and writes in a transaction are executed as one operation: either the entire
    transaction succeeds (commit) or it fails (abort, rollback)
  * If a transaction fails, the application can safely retry

## Glossary
* **_ACID_**: which stands for **_Atomicity_**, **_Consistency_**, **_Isolation_** and **_Durability_**.
  * **_Atomicity_**: The ability to abort a transaction on error and have all writes from that transaction discarded.
  * **_Consistency_**: Certain statements about your data (invariants) that must always be true.
  * **_Isolation_**: Concurrently executing transactions are isolated from each other.
  * **_Durability_**: The promise that once a transaction has committed successfully, 
any data it has written will not be forgotten, even if there is hardware fault or database crashes.
* **_BASE_**: **_Basically Available_**, **_Soft state_**, and **_Eventual consistency_**.
* **_Serializable isolation_**: The database guarantees that transactions have the same effect as if they ran serially 
(i.e. one at a time, without any concurrency).
* **_Dirty reads_**: Uncommitted data is visible by other transactions.
  * Most database prevents dirty reads by storing both of old committed value and the new value set by the transaction 
that holds the write lock.
  * While a transaction is ongoing, any other transactions that read the object simply given the old value.
* **_Dirty writes_**: The uncommitted writes of a transaction are overridden by other transactions.
  * Most database prevents dirty writes by using row-level locks.
* **_Read committed_**: The isolation level for transactions: no dirty reads and no dirty writes.
* **_Read skew_**: The same query requested by user seeing inconsistent results from database.
  * Read skew is an example of a **_nonrepeatable read_**.
* **_Snapshot isolation_**: Each transaction reads from a consistent snapshot - all committed data in the database 
at the start of the transaction.
  * Snapshot isolation is the solution for read skew.
  * A key principle of snapshot isolation is readers never block writers, and writers never block readers.
* **_Multi-version concurrency control (MVCC)_**: The technique of databases maintains several versions of an object 
side by side.
  * This is the implementation for snapshot isolation.
* **_Lost update_**: Two transaction concurrently do read-modify-write some values, one modification can be lost, 
this problem is called lost update.
* **_Write skew_**: Different users read the same objects and update some of those objects (writes might have no 
overlap) but resulting in violating system rules.
* **_Phantoms_**: The effect where a write in one transaction changes the result of a search query in another 
transaction.
* **_Stored procedure_**: The transaction code that application stores in database for a single-thread serial 
transaction processing.
* **_Premise_**: a fact that was true at the beginning of the transaction.
* **_Serializable Snapshot Isolation (SSI)_**: A fairly new algorithm that avoids most of the downsides of the previous 
approaches. It uses an optimistic approach, allowing transactions to proceed without blocking. 
When a transaction wants to commit, it is checked, and it is aborted if the execution was not serializable.

### Dirty Reads
Violating isolation: one transaction reads another transaction's uncommitted writes (a “dirty read”).
![Violating isolation: one transaction reads another transaction's uncommitted writes (a “dirty read”).](https://ebrary.net/htm/img/15/554/67.png)

**Solution**: For every object that is written, the database remembers both the old committed value and the new value 
set by the transaction that currently holds the write lock. While the transaction is ongoing, any other transaction that
read the object are simply given the old value. Only when the new value is committed do transactions switch over to 
reading the new value.

No dirty reads: user 2 sees the new value for x only after user 1’s transaction has committed.
![No dirty reads: user 2 sees the new value for x only after user 1’s transaction has committed.](https://ebrary.net/htm/img/15/554/69.png)

### Dirty Writes
With dirty writes, conflicting writes from different transactions can be mixed up.
![With dirty writes, conflicting writes from different transactions can be mixed up.](https://ebrary.net/htm/img/15/554/70.png)

**Solution**: Most commonly, database prevent dirty writes by using row-level locks: when a transaction wants to modify 
a particular object (row or document), it must first acquire a lock on that object. It must then hold that lock until 
the transaction is committed or aborted.

### Read Committed
**Read Committed = No Dirty Reads + No Dirty Writes**

### Read Skew
Read skew: Alice observes the database in an inconsistent state. 

Alice might her total balance in her two accounts is $900 at some point when money transfer was processing at the half. 
![Read skew: Alice observes the database in an inconsistent state.](https://ebrary.net/htm/img/15/554/71.png)

Read skew is not a lasting problem, because the inconsistency will eventually be resolved, but some situations 
cannot tolerate such temporary inconsistency:
* Backups
* Analytic queries and integrity checks

**Solution**: Using snapshot isolation, the idea is that each transaction reads from a consistent snapshot of 
the database—that is, the transaction sees all the data that was committed in the database at the start of the 
transaction. Even if the data is subsequently changed by another transaction, each transaction sees only the old data 
from that particular point in time.

### Snapshot Isolation
Implementing snapshot isolation using multi-version objects
![Implementing snapshot isolation using multi-version objects.](https://ebrary.net/htm/img/15/554/73.png)

* When a transaction is started, it is given a unique, **always-increasing** **transaction ID (txid)**. Whenever a 
transaction writes anything to the database, the data it writes is tagged with the transaction ID of the writer.
* Each row in a table has a **created_by** field, containing the ID of the transaction that inserted this row into the
table. 
* **An update is internally translated into a delete and a create.** Each row has a **deleted_by** field, which is 
initially empty. 
  * If a transaction deletes a row, the row isn’t actually deleted from the database, but it is marked for deletion by 
setting the deleted_by field to the ID of the transaction that requested the deletion. 
  * At some later time, when it is certain that no transaction can any longer access the deleted data, a garbage 
collection process in the database removes any rows marked for deletion and frees their space.
* **Any writes made by transactions with a later transaction ID** (i.e., which started after the current transaction 
started) **are ignored**, regardless of whether those transactions have committed.

### Lost Update
A race condition between two clients concurrently incrementing a counter.
![](https://ebrary.net/htm/img/15/554/66.png)

User1's increasing counter update was lost due to User2's override.

**Solutions**:

1. Cursor stability: Using an **exclusive lock** on the object when it is read so that no other transaction can read it 
until the update has been applied.
2. Force all atomic operations to be executed **on a single thread**.
3. Using **explict locking** defined at application level on objects.
4. Transaction manager **detects** lost update and **abort** the transaction and **forces it to retry** its 
read-modify-write cycle.
5. **Compare-and-set**: Allowing an update to happen only if the value has not changed since you last read it.

Techniques based on locks or compare-and-set do not apply in databases with multi-leader or leaderless replication.

Atomic operations can work well in a replicated context, especially if they are **commutative**. For example, 
incrementing a counter or adding an element to a set are commutative operations. When a value is concurrently 
updated by different clients, automatically merges together the updates in such a way that no updates are lost.

### Write Skew
Example of write skew causing an application bug.
![Example of write skew causing an application bug.](https://ebrary.net/htm/img/15/554/74.png)

Two users updated their own on-call shifts but resulted in violating on-call system requirement: 
having at least one doctor on call.

The pattern of write skew:
1. A SELECT query checks whether some requirement is satisfied by searching for rows that match some search condition.
2. Depending on the result of the first query, the application code decides how to continue.
3. If the application decides to go ahead, it makes a write (INSERT, UPDATE, or DELETE) to the database and commits 
the transaction.

The write skew problem is your step 1's requirements are no longer satisfied because of other transactions' writes when 
you are doing step 3.

### Actual Serial Execution
The simplest way of avoiding concurrency problems is to **remove the concurrency entirely: to execute only one 
transaction at a time, in serial order, on a single thread**.

The approach of executing transactions serially is implemented in VoltDB/H-Store, Redis, and Datomic. 

A system designed for single-threaded execution can sometimes perform better than a system that supports concurrency, 
because it can avoid the coordination overhead of locking. 
However, its throughput is limited to that of a single CPU core. In order to make the most of that single thread, 
transactions need to be structured differently from their traditional form.

* **Every transaction must be small and fast**, because it takes only one slow transaction to stall all transaction 
processing.
* It is limited to use cases where **the active dataset can fit in memory**. Rarely accessed data could potentially 
be moved to disk, but if it needed to be accessed in a single-threaded transaction, the system would get very slow.
* **Write throughput must be low enough to be handled on a single CPU core**, or else transactions need to be 
partitioned without requiring cross-partition coordination.
* Cross-partition transactions are possible, but there is a hard limit to the extent to which they can be used.

### Two-Phase Locking (2PL)
**Two-Phase Locking**: Several transactions are allowed to concurrently read the same object as long as nobody 
is writing to it:
* If transaction A has read an object and transaction B wants to write to that object, B must wait until 
a commits or aborts before it can continue.
* If transaction A has written an object and transaction B wants to read that object, B must wait until 
a commits or aborts before it can continue.

The blocking of readers and writers is implemented by a having a lock on each object in the database. 
The lock can either be in **_shared mode_** or in **_exclusive mode_**. a transaction wants to read an object, 
it must first acquire the lock in shared mode. Several transactions are allowed to hold the lock in shared mode 
simultaneously, but if another transaction already has an exclusive lock on the object, these transactions must wait.

Since so many locks are in use, it can happen quite easily that transaction A is stuck waiting for 
transaction B to release its lock, and vice versa. This situation is called **_deadlock_**. The database automatically 
detects deadlocks between transactions and aborts one of them so that the others can make progress.

The big downside of two-phase locking, and the reason why it hasn’t been used by everybody since the 1970s, 
is performance: **transaction throughput and response times of queries are significantly worse under two-phase locking 
than under weak isolation**.

**predicate lock** provide locking for to all objects that match some search condition rather than a particular object 
(e.g., one row in a table). The key idea here is that a predicate lock applies even to objects that do not yet 
exist in the database.

If two-phase locking includes predicate locks, the database prevents all forms of write skew and other race conditions, 
and so its isolation becomes serializable.

Unfortunately, predicate locks do not perform well: if there are many locks by active transactions, 
checking for matching locks becomes time-consuming. For that reason, most databases with 2PL actually implement 
**index-range locking** (also known as **next-key locking**), which is a simplified approximation of predicate locking.

### Serializable Snapshot Isolation (SSI)
How does the database know if a query result might have changed?
* Detecting reads of a stale MVCC object version (uncommitted write occurred before the read)
* Detecting writes that affect prior reads (the write occurs after the read)

**Detecting when a transaction reads outdated values from an MVCC snapshot.**
![Detecting when a transaction reads outdated values from an MVCC snapshot.](https://ebrary.net/htm/img/15/554/77.png)

**In serializable snapshot isolation, detecting when one transaction modifies another transaction's reads.**
![In serializable snapshot isolation, detecting when one transaction modifies another transaction's reads.](https://ebrary.net/htm/img/15/554/78.png)

Compared to two-phase locking, the big advantage of serializable snapshot isolation is that one transaction doesn’t 
need to block waiting for locks held by another transaction. 

Compared to serial execution, serializable snapshot isolation is not limited to the throughput of a single CPU core.


