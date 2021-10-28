##Data Models

### Relationships

* **Many-to-One (m:1)**
  * e.g. Many people live in one particular region.
  * m:1 schema can be used for database normalizing, for example, storing region ID in person table, and join with region table when needed.
  * Fits for relational database.
* **One-to-Many (1:m)**
  * Self-contained tree structure like JSON.
  * For example, a person's LinkedIn profile can have multiple work experiences in different companies.
* **Many-to-Many (m:n)**
  * A graph datastore offers nodes and edges.
  * For example, a person node can have many incoming and outgoing relationships(edges), Jimmy -> live_in -> USA, Jimmy -> work_for -> Facebook.

### Databases

|   |Relational|Document|Graph|
|---|---|---|---|
|Many-to-One   |Good   |Weak   |Good   |
|One-to-Many   |Weak   |Good   |Weak   |
|Many-to-Many   |Good   |Weak   |Good   |
|Schema Flexibility|With Schema   |No Schema   |No Schema   |
|Use Cases|Tables   |Web rendering   |Social graph, networks, web graph   |
|Locality   |Improved by grouping   |Strong   |Weak   |

"**NoSQL**" datastores have diverged in two main directions:
1. **Document databases** target use cases where data comes in self-contained documents and relationships between document and another are rare.
2. Graph databases go in the opposite direction, targeting use cases where anything is potentially related to everything.

Both of document and graph database don't enforce a schema, which can make it easier to adapt applications to changing requirements.

The relational model provides better support for joins, and many-to-one and many-to-many relationships.