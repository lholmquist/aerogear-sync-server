## AeroGear Server Differential Synchronization Server Core
This module contains implementations for the [server side API](../api)

### ServerSyncEngine
The [ServerSyncEngine](./src/main/java/org/jboss/aerogear/sync/server/ServerSyncEngine.java) is the class that drives
the server side. This is the class that a server implementation would use by [adding documents](./src/main/java/org/jboss/aerogear/sync/server/ServerSyncEngine.java#L53)
to the engine, and then later to have the engine [generate edits](./src/main/java/org/jboss/aerogear/sync/server/ServerSyncEngine.java#L89)
that can be sent to the server side for processing. The engine also handles the revers, that is when edits are recieved
from the server they are processed by the engines [patch](./src/main/java/org/jboss/aerogear/sync/server/ServerSyncEngine.java#L101)
method.

When the server engine is created it is passed an implementation of [ServerSynchronizer](../api/src/main/java/org/jboss/aerogear/sync/server/ServerSynchronizer.java)
 , and [ServerDataStore](../api/src/main/java/org/jboss/aerogear/sync/server/ServerDataStore.java) which allows for
 changing the underlying synchronization algorithm and the storage implementation respectively.

#### DefaultServerSynchronizer
The [DefaultServerSynchronizer](./src/main/java/org/jboss/aerogear/sync/server/DefaultServerSynchronizer.java) is an
implementation of [ServerSynchronizer](../api/src/main/java/org/jboss/aerogear/sync/server/ServerSynchronizer.java) that
can handle text documents. It uses [google-diff-match-patch](https://code.google.com/p/google-diff-match-patch/) with some minor tweaks.

#### ServerInMemoryDataStore
The [ServerInMemoryDataStore](./src/main/java/org/jboss/aerogear/sync/server/ServerInMemoryDataStore.java) is an
implementation of [ServerDataStore](../api/src/main/java/org/jboss/aerogear/sync/server/ServerDataStore.java) that
storing all data in-memory.


