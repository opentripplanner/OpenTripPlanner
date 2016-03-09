# There are several snapshot dependencies in OTP

Release of OTP was complicated to perform, as it has several
snapshot dependencies.

You will find them by doing:
`mvn dependency:tree | grep SNAPSHOT`

I need to replace these with release versions, and also include their transitive
depdencies.

# The packages:

The following two turned out not to have any changes:
```
org.onebusaway:onebusaway-gtfs:jar:1.3.5-conveyal-SNAPSHOT
org.onebusaway:onebusaway-csv-entities:jar:1.1.5-SNAPSHOT:compile # Really a transitive dependency
```

The rest have changes
```
crosby.binary:osmpbf:jar:1.3.4-SNAPSHOT:compile
com.conveyal:osm-lib:jar:0.1-SNAPSHOT:compile # A transitive dependency to a relase package?!
com.conveyal:r5:jar:0.1-SNAPSHOT:compile
com.conveyal:gtfs-lib:jar:0.2-SNAPSHOT:compile # Really a transitive dependency
```

The transitive dependencies have been marked in pom file.

## Operations

**r5** is a pain in the ass, as it does not have a release.
The same goes for several of the other conveyal packages.

I find the latest snapshot like this:
`ls -lart ~/.m2/repository/com/conveyal/r5/0.1-SNAPSHOT`

Deploying the jar with maven failed, so I do it manually:

```
cd ~/.m2/repository/com/conveyal/r5/0.1-SNAPSHOT
scp r5-0.1-20160307.111216-85.jar nexus:.
ssh nexus
mkdir -p /opt/mavenrepo/com/conveyal/r5/0.1-20160307.111216-85
mv r5-0.1-20160307.111216-85.jar /opt/mavenrepo/com/conveyal/r5/0.1-20160307.111216-85/.

cd ~/.m2/repository/com/conveyal/gtfs-lib/0.2-SNAPSHOT
scp gtfs-lib-0.2-20160307.194541-8.jar nexus:.
ssh nexus
mkdir -p /opt/mavenrepo/om/conveyal/gtfs-lib/0.2
 mv gtfs-lib-0.2-20160307.194541-8.jar /opt/mavenrepo/om/conveyal/gtfs-lib/0.2

cd ~/.m2/repository/crosby/binary/osmpbf/1.3.4-SNAPSHOT
scp osmpbf-1.3.4-20150914.191218-1.jar  nexus:.
ssh nexus
mkdir -p /opt/mavenrepo/crosby/binary/osmpbf/1.3.4
mv osmpbf-1.3.4.jar /opt/mavenrepo/crosby/binary/osmpbf/1.3.4-20150914.191218-1/.
```

Update pom.xml in the appropriate place by commenting out the snapshot
dependency, and using the explicit one instead.


# Extra deps:

Need to make sure the following dependencies are present and OK.
(They were - I compared output from `mvn dependency:tree` before
  and after.)

```
[INFO] +- org.onebusaway:onebusaway-gtfs:jar:1.3.5-conveyal-SNAPSHOT:compile
[INFO] |  \- org.onebusaway:onebusaway-csv-entities:jar:1.1.5-SNAPSHOT:compile
[INFO] |     \- commons-beanutils:commons-beanutils:jar:1.7.0:compile
```

```
[INFO] +- io.opentraffic:traffic-engine:jar:0.2:compile
[INFO] |  +- com.conveyal:osm-lib:jar:0.1-SNAPSHOT:compile
[INFO] |  |  +- org.mapdb:mapdb:jar:1.0.6:compile
[INFO] |  |  \- org.openstreetmap.osmosis:osmosis-osm-binary:jar:0.43.1:compile
[INFO] |  +- org.apache.commons:commons-csv:jar:1.1:compile
[INFO] |  +- com.github.ben-manes.caffeine:caffeine:jar:1.2.0:compile
[INFO] |  |  +- com.github.ben-manes.caffeine:tracing-api:jar:1.2.0:compile
[INFO] |  |  \- com.google.code.findbugs:jsr305:jar:3.0.0:compile
```

```
[INFO] \- com.conveyal:r5:jar:0.1-SNAPSHOT:compile
[INFO]    +- org.geotools:gt-opengis:jar:14.0:compile
[INFO]    |  \- net.java.dev.jsr-275:jsr-275:jar:1.0-beta-2:compile
[INFO]    +- com.conveyal:gtfs-lib:jar:0.2-SNAPSHOT:compile
[INFO]    \- com.sparkjava:spark-core:jar:2.3:compile
[INFO]       +- org.slf4j:slf4j-simple:jar:1.7.12:compile
[INFO]       +- org.eclipse.jetty:jetty-server:jar:9.3.2.v20150730:compile
[INFO]       |  +- javax.servlet:javax.servlet-api:jar:3.1.0:compile
[INFO]       |  +- org.eclipse.jetty:jetty-http:jar:9.3.2.v20150730:compile
[INFO]       |  |  \- org.eclipse.jetty:jetty-util:jar:9.3.2.v20150730:compile
[INFO]       |  \- org.eclipse.jetty:jetty-io:jar:9.3.2.v20150730:compile
[INFO]       +- org.eclipse.jetty:jetty-webapp:jar:9.3.2.v20150730:compile
[INFO]       |  +- org.eclipse.jetty:jetty-xml:jar:9.3.2.v20150730:compile
[INFO]       |  \- org.eclipse.jetty:jetty-servlet:jar:9.3.2.v20150730:compile
[INFO]       |     \- org.eclipse.jetty:jetty-security:jar:9.3.2.v20150730:compile
[INFO]       +- org.eclipse.jetty.websocket:websocket-server:jar:9.3.2.v20150730:compile
[INFO]       |  +- org.eclipse.jetty.websocket:websocket-common:jar:9.3.2.v20150730:compile
[INFO]       |  \- org.eclipse.jetty.websocket:websocket-client:jar:9.3.2.v20150730:compile
[INFO]       \- org.eclipse.jetty.websocket:websocket-servlet:jar:9.3.2.v20150730:compile
[INFO]          \- org.eclipse.jetty.websocket:websocket-api:jar:9.3.2.v20150730:compile
```
