# com.conveyal.r5

**r5** is a pain in the ass, as it does not have a release.

I find the latest snapshot like this:
`ls -lart ~/.m2/repository/com/conveyal/r5/0.1-SNAPSHOT`

Deploying this with maven failed, so I do it manually:
```
cd ~/.m2/repository/com/conveyal/r5/0.1-SNAPSHOT
scp r5-0.1-20160307.111216-85.jar nexus:.
ssh nexus
mkdir -p /opt/mavenrepo/com/conveyal/r5/0.1-20160307.111216-85
mv r5-0.1-20160307.111216-85.jar /opt/mavenrepo/com/conveyal/r5/0.1-20160307.111216-85/.

# Same procedure for onebusaway-gtfs
cd ~/.m2/repository/org/onebusaway/onebusaway-gtfs/1.3.5-conveyal-SNAPSHOT
scp onebusaway-gtfs-1.3.5-conveyal-20151030.144540-8.jar nexus:.
ssh nexus
mkdir -p /opt/mavenrepo/org/onebusaway/onebusaway-gtfs/1.3.5-conveyal-20151030.144540-8
mv onebusaway-gtfs-1.3.5-conveyal-20151030.144540-8.jar /opt/mavenrepo/org/onebusaway/onebusaway-gtfs/1.3.5-conveyal-20151030.144540-8/.

cd ~/.m2/repository/crosby/binary/osmpbf/1.3.4-SNAPSHOT
scp osmpbf-1.3.4-20150914.191218-1.jar  nexus:.
ssh nexus
mkdir -p /opt/mavenrepo/crosby/binary/osmpbf/1.3.4-20150914.191218-1
mv osmpbf-1.3.4-20150914.191218-1.jar /opt/mavenrepo/crosby/binary/osmpbf/1.3.4-20150914.191218-1/.

```

Update pom.xml in the appropriate place.
