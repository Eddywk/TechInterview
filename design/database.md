# Database

## Index
An index is an additional structure that is derived from the primary data.

Many databases allow you to add and remove indexes, and this doesn't affect the contents of the database; 
it only affects the performance of queries.

Maintaining additional structures incurs overhead, especially on writes. 
This is an important trade-off in storage systems: 
well-chosen indexes speed up read queries, but every index slows down writes.

### Hash Indexes

The very simple hash index implementation, you will need two components:
1. Data File: An append-only data file on disk
2. Hash Map: A in-memory hash map where every key is mapped to a byte offset in the data file

**Write Operation:**

Whenever you append a new key-value pair to the data file, 
you also update the hash map to reflect the offset of the data you just wrote.

**Read Operation:**

When you want to look up a value, use the hash map to find the offset in the data file, 
seek to that location, and read the value.

**Use Case:**

This simplistic but viable approach is suited well to **situations where the value for each key in updated frequently**. 
For example, the key might be the URL of a YouTube video, and value might be the number of times it has been played 
(incremented every time someone hits the play button).


**How do we avoid running out of disk space issue?**

There are not too many distinct keys, so we don't need to worry about the size of our hash map in memory.
But our storage system is append-only file, so for each key, there will be a lot of writes. 
We will eventually run into out of disk space issue if we keep appending data to data file.

The solution of resolving this issue for hash index storage system is **segments** + **compaction**.

A **segment** here is a certain size storage block.

**Compaction** means throwing away duplicate keys in the log, and keep only the most recent update for each key. 
(By merging previous segments)

**Some issues in real implementation:**

**File Format**

It's faster and simpler to use a binary format that first encodes the length of a string in bytes, 
followed by the raw string.

**Deleting Records**

For deleting a key, you have to append a special deletion record to the data file (sometimes called a **tombstone**).
When log segments are merged, 
the tombstone tells the merging process to **discard** any previous values for the deleted key.

**Crash Recovery**

If the database restarted, the in-memory hash maps are lost.

In principle, you can restore each segment's hash map by reading the entire segment file from beginning to end, 
and noting the offset of the most recent value for every key as you scan. (Data file -> restore -> hash map)

To speed up recovery, you can store a snapshot of each segment's hash map on disk, 
which can be loaded into memory more quickly.(Hash map -> snapshot on disk -> hash map)

**Partially Written Records**

Database may crash at any time, there might be halfway data appended to log.

We can use **checksums** to detect and ignore corrupted parts of log file.

Checksum is a hash algorithm to generate small-sized block from transmitted data for the purpose of detecting errors
that may have been introduced during its transmission or storage. 
(See [example](https://www.lifewire.com/what-does-checksum-mean-2625825))

**Concurrency Control**

For writes, a common implementation choice is to have **only one writer thread**.

For reads, data file segments are append-only and otherwise immutable, 
so they can be read concurrently by multiple threads.

**Append-only Design Advantages**

In comparing with update data file in place and overwriting the old value with one new, 
an append-only design turns out to be good for several reasons:

* Appending and segment merging (compaction) are sequential writes operations, 
which are generally much faster than random writes, especially for both of magnetic spinning-disk hard drives and SSDs.
* Concurrency and crash recovery are much simpler for append-only or immutable. 
For example, if you choose update file in place, when crash happens while a value was being overwritten, 
leaving you the value with half old and half new content, you don't know how to recover it.
* Merging old segments avoids the problem of data files getting fragmented over time.

**Limitations of Hash Index**
* The hash map must fit in memory.
In principle, maintaining a hash map on disk requires a lot of random access I/O, 
it's expensive to grow when it becomes full, and cause hash collisions on disk address.
* Range queries are not efficient.
You cannot easily scan over all keys within a range, you have to look up all keys individually in the hash maps.
