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
-Dbootstrap.httpPort (bootstrap server httpPort)
#### server
-Dserver.host (application host address)
-Dserver.httpPort (application server httpPort - for spring boot http server)
-Dserver.name (application server unique username)
#### cluster manager
-Dcluster-manager.ttl (time to live value for inactive noded)

