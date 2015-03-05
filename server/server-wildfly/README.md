## Java EE Sync Server

This module embeds the [ServerSyncEngine](../server-engine/src/main/java/org/jboss/aerogear/sync/server/ServerSyncEngine.java) from [server-engine](../server-engine) and adds network connectivity by exposing WebSockets through the [JSR 356](https://jcp.org/en/jsr/detail?id=356) APIs.

At the moment the WAR file only supports [JSON Patch](../..//synchronizers/json-patch).


### Getting started

Build the WAR file using Maven

    mvn clean install


### Deployment

To run the server just deploy the WAR file to WildFly


    mvn wildfly:deploy

Have fun!