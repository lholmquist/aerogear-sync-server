## AeroGear Server Differential Synchronization Client Core
This module contains implementations for the [client side API](../api)

### ClientSyncEngine
The [ClientSyncEngine](./src/main/java/org/jboss/aerogear/sync/client/ClientSyncEngine.java) is the class that drives
the client side. This is the class that clients interact with by [adding documents](./src/main/java/org/jboss/aerogear/sync/client/ClientSyncEngine.java#L51)
to the engine, and then later to have the engine [generate edits](./src/main/java/org/jboss/aerogear/sync/client/ClientSyncEngine.java#L70)
that can be sent to the server side for processing. The engine also handles the revers, that is when edits are recieved
from the server they are processed by the engines [patch](./src/main/java/org/jboss/aerogear/sync/client/ClientSyncEngine.java#L88)
method.

When the client engine is created it is passed an implementation of [ClientSynchronizer](../api/src/main/java/org/jboss/aerogear/sync/client/ClientSynchronizer.java)
 , and [ClientDataStore](../api/src/main/java/org/jboss/aerogear/sync/client/ClientDataStore.java) which allows for
 changing the underlying synchronization algorithm and the storage implementation respectively.

#### DefaultClientSynchronizer
The [DefaultClientSynchronizer](./src/main/java/org/jboss/aerogear/sync/client/DefaultClientSynchronizer.java) is an
implementation of [ClientSynchronizer](../api/src/main/java/org/jboss/aerogear/sync/client/ClientSynchronizer.java) that
can handle text documents. It uses [google-diff-match-patch](https://code.google.com/p/google-diff-match-patch/) with some minor tweaks.

#### ClientInMemoryDataStore
The [ClientInMemoryDataStore](./src/main/java/org/jboss/aerogear/sync/client/ClientInMemoryDataStore.java) is an
implementation of [ClientDataStore](../api/src/main/java/org/jboss/aerogear/sync/client/ClientDataStore.java) that
storing all data in-memory.


