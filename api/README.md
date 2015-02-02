## AeroGear Server Differential Synchronization API
This module contains interfaces for both server and client side classes.

### Client API
The [client package](./src/main/java/org/jboss/aerogear/sync/client) contains two interfaces:

#### ClientSynchronizer
The [ClientSynchronizer](./src/main/java/org/jboss/aerogear/sync/client/ClientSynchronizer.java) defines the methods
required to perform the specific differential synchronization steps.

#### ClientDataStore
The [ClientDataStore](./src/main/java/org/jboss/aerogear/sync/client/ClientDataStore.java) defines the methods for
storing the different document to allow for different types of storage models.

### Server API
The [server package](./src/main/java/org/jboss/aerogear/sync/server) contains two interfaces:

#### ServerSynchronizer
The [ServerSynchronizer](./src/main/java/org/jboss/aerogear/sync/server/ServerSynchronizer.java) defines the methods
required to perform the specific differential synchronization steps.

#### ServerDataStore
The [ServerDataStore](./src/main/java/org/jboss/aerogear/sync/server/ServerDataStore.java) defines the methods for
storing the different document to allow for different types of storage models.

### Common API
These interfaces are common to both the server and client API and are made up of the various types of documents, diffs
, and edits.


