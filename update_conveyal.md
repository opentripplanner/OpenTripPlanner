# com.conveyal.r5

**r5** is a pain in the ass, as it does not have a release.

I find the latest snapshot like this:
`ls -lart ~/.m2/repository/com/conveyal/r5/0.1-SNAPSHOT`

Deploying this with maven failed, so I do it manually:
```
cd ~/.m2/repository/com/conveyal/r5/0.1-SNAPSHOT
scp r5-0.1-20160307.111216-85.jar nexus:.
mkdir -p /opt/mavenrepo/com/conveyal/r5/0.1-20160307.111216-85
mv r5-0.1-20160307.111216-85.jar /opt/mavenrepo/com/conveyal/r5/0.1-20160307.111216-85/.
```

Update pom.xml in the appropriate place.
