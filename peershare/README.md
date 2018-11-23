# PeerShare
Distributed content sharing application  
Distributed Systems (CS4262) - Group Project

## How to run
Run application by providing base path where files located  
```
java -jar peershare-1.0-SNAPSHOT.jar
```

## Config options available
#### file management
-Dfile-store.input-file (file contains file names to index)
#### cluster management
-Dbootstrap.host (bootstrap server address)
-Dbootstrap.port (bootstrap server port)
#### server
-Dserver.host (application host address)
-Dserver.port (application server port - spring boot http port)
-Dserver.name (application server unique username)
#### cluster
-Dnode.ttl (time to live value for inactive nodes)
-Dnode.hear-beat-interval (interval to send heart beats)
#### messages
-Dgossip.mx-hops (hops limit to terminate gossip messages)
-Dsearch.max-hops (hops limit to terminate search messages)
-Dsearch.timeout (maximum search waiting time)
-Dsearch.results (web socket topic to push search search results)

