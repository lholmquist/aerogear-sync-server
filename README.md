## AeroGear Data Synchronization Project [![Build Status](https://travis-ci.org/aerogear/aerogear-sync-server.png)](https://travis-ci.org/aerogear/aerogear-sync-server)
This project in an implementation of Google's [Differential Synchonrization](http://research.google.com/pubs/pub35605.html) 
by Neil Fraser, that can be used by AeroGear's client libraries.

## Project Info

|                 | Project Info  |
| --------------- | ------------- |
| License:        | Apache License, Version 2.0  |
| Build:          | Maven  |
| Documentation:  | http://aerogear.org/sync/  |
| Issue tracker:  | https://issues.jboss.org/browse/AGSYNC  |
| Mailing lists:  | [aerogear-users](http://aerogear-users.1116366.n5.nabble.com/) ([subscribe](https://lists.jboss.org/mailman/listinfo/aerogear-users))  |
|                 | [aerogear-dev](http://aerogear-dev.1069024.n5.nabble.com/) ([subscribe](https://lists.jboss.org/mailman/listinfo/aerogear-dev))  |

## AeroGear Sync
AeroGear Sync consists of the following modules:

* [api](./api)  
Contains the interfaces for the server and client, plus definitions of common types.

* [core](./core)  
Contains a base implementation of the common types from the [api](./api) module. These are used by both the
[client-engine](./client/client-engine) and the [server-engine](./server/server-engine) modules.

* [server](./server)  
Contains a concrete implementation of the server side sync engine API. This implementation is intended to be "embedded" in a server
component, like a Servlet, a Netty server, etc.

* [client](./client)  
Contains a concrete implementation of the client side sync engine API. This implementation is intended to be "embedded" in a client
component, like a Netty client, an Android device, etc.

* [synchronizers](./synchronizers)  
Contains implementations that perform the synchronization operations in the Differential Synchronization algorithm.

* [distribution](./distribution)  
This module produces an executable jar. See _Creating a distribution_ below for more information

* [itests](./itests)  
Contains tests that use both the [server-engine](./server/server-engine) and [client-engine](./client/client-engine) in cooperation.

## Building

    mvn clean install

## Usage

### Starting the JSON Patch server

    cd server/server-netty
    mvn exec:exec -Pjsonpatch
    
### Starting the DiffMatchPatch server

    cd server/server-netty
    mvn exec:exec -Pdiffmatch

## Documentation

For more details about the current release, please consult [our documentation](http://aerogear.org/sync).

### Resources
* [Differential Synchronization presentation](https://www.youtube.com/watch?v=S2Hp_1jqpY8)
* [Differential Synchronization paper](http://research.google.com/pubs/pub35605.html)
* [Differential Synchronization step by step keynote presentation](https://www.icloud.com/iw/#keynote/BAKHgqmqd5ETPe9ebKyBhSINoBo1QHaNPYeF/diffsync)

## Development

If you would like to help develop AeroGear you can join our [developer's mailing list](https://lists.jboss.org/mailman/listinfo/aerogear-dev), join #aerogear on Freenode, or shout at us on Twitter @aerogears.

Also takes some time and skim the [contributor guide](http://aerogear.org/docs/guides/Contributing/)

## Questions?

Join our [user mailing list](https://lists.jboss.org/mailman/listinfo/aerogear-users) for any questions or help! We really hope you enjoy app development with AeroGear!

## Found a bug?

If you found a bug please create a ticket for us on [Jira](https://issues.jboss.org/browse/AGSYNC) with some steps to reproduce it.

