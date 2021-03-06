# Data System Concepts

## Reliability
**Reliability** for software is the expectations including:
* The application performs the function that the user expected
* The application can tolerate user mistake and misuse.
* The application's performance can handle expected load and data volume.
* The application prevents any unauthorized access and abuse.

So reliability means **"continuing to work correctly, even when things go wrong"**.

A **fault** is usually defined as one component of the system deviating from its spec.

A **failure** is when the system as a whole stops providing the required service to the user.

### Hardware Faults
Hard disks are reported as having a **mean time to failure** (**MTTF**) of about 10 to 50 years. Thus, on a storage cluster with 10,000 disks, we should expect on average one disk to die per day.

Solutions:

1. Add redundancy to the individual hardware components in order to reduce the failure rate of the system.
2. Move toward systems that can tolerate the loss of entire machines. e.g. Having planned downtime for reboot.

### Software Errors
Lots of small things can help: carefully thinking about assumptions and interactions in the system; Thorough testing; process isolation; allowing processes to crash and restart; measuring. monitoring, and analyzing system behavior in production; Raising an alert if breaching SLA.

### Human Errors
Solutions:

* **Design systems in a way that minimize opportunities for error.**
  * For example, well-designed abstractions, APIs and admin interface, etc.
* **Decouple the places where people make the most mistakes from the places where they can cause failures.**
  * E.g. Provide fully featured non-production _sandbox_ environments.
* **Test thoroughly at all levels, from unit tests to whole-system integration tests and manual tests.**
* **Allow quick and easy recovery from human errors, to minimize the impact in the cause of a failure.**
  * For example, fast rollback configuration changes and gradually roll out new code, and provide tools to recompute data if old computation was incorrect.
* **Set up detailed and clear monitoring.**
  * Such as performance metrics and error rates.
* **Implement good management practices and training.**

## Scalability
**Scalability** means having strategies for keeping performance good, even when load increases.

### Describing Load
Load can be described with a few numbers which we call **load parameters**: TPS, the ratio of reads to writes in a database, concurrency, hit rate on a cache.

### Describing Performance
**Throughput** - the number of records we can process per second, or the total time it takes to run a job on a dataset of a certain size.

### Approaches for Coping with Load
1. Scaling up, vertical scaling, moving to more powerful machine.
2. Scaling out, horizontal scaling, distributing the load across multiple smaller machines.

## Maintainability
**Maintainability** has many facets, but in essence it's about making life better the engineering and operations teams who need to work with the system.

* **Operability**: Make it easy for operations team to keep the system running smoothly.
* **Simplicity**: Make it easy for new engineers to understand the system, by removing as much complexity as possible from the system.
* **Evolvability**: Make it easy for engineers to make changes to the system in the future, adapting it for unanticipated use cases as requirements change.

