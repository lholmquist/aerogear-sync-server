## Netty Differential Synchronization Server Implementation
This module contains a server implementations that uses the [server-core](../server-core)

### DiffMatchPatchSyncServer
The [DiffMatchPatchSyncServer](./src/main/java/org/jboss/aerogear/sync/DiffMatchPatchSyncServer.java) is a standalone server implementation
that uses [DiffMatchPatch](../../synchronizers/diffmatchpath) for diffs/patches

#### Starting using Maven

    mvn exec:exec -Pdiffmatch

### JsonPatchSyncServer
The [JsonPatchSyncServer](./src/main/java/org/jboss/aerogear/sync/JsonPatchSyncServer.java) is a standalone 
server implementation that uses [JSON Patch](../..//synchronizers/json-patch) for diff/patches.

#### Starting using Maven

    mvn exec:exec -Pjsonpatch
    
### JsonMergePatchSyncServer
The [JsonMergePatchSyncServer](./src/main/java/org/jboss/aerogear/sync/JsonMergePatchSyncServer.java) is a standalone 
server implementation that uses [JSON Merge Patch](../../synchronizers/json-merge-patch) for diff/patches.

#### Starting using Maven

    mvn exec:exec -Pjsonmerge

### Configuration option
The server can be configured using [sync.config](./src/main/resources/sync.config)
Example config:

    {
        "host": "0.0.0.0",
        "port": 7777,
        "gcm": { "enabled": false, 
                 "host", "gcm.googleapis.com"",
                 "port", 5235
                 "senderId": 123456, 
                 "apiKey": "XXXXXXXXXXX"
        }
    }
    
__host__  
The host that the server will bind to.

__port__  
The port that the server will bind to.

#### Google Cloud Messaging (GCM) configuration options  

__enabled__  
Determines whether the GCM server should be started.

__host__  
The GCM host. Default is ```gcm.googleapis.com```.

__port__
The GCM port. Default is ```5235```.


__senderId__  
The senderId, is the ```Project Number``` in [Google Developer Console](https://console.developers.google.com).
Retrieve a senderId from [Google](https://developer.android.com/google/gcm/gs.html)

__apiKey__  
This is the ```API KEY``` credential created for the server application in [Google Developer Console](https://console.developers.google.com).
Retrieve a apiKey from [Google](https://developer.android.com/google/gcm/gs.html)


### DiffSyncHandler
[DiffSyncHandler](./src/main/java/org/jboss/aerogear/sync/DiffSyncHandler.java) is a Netty handler responsible for
delegating requests to the sync engine and sending back responses. It also handles networking tasks like reconnects etc.



