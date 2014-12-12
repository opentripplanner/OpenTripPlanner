# Getting OpenTripPlanner

## Pre-built JARs

OpenTripPlanner is now distributed as a single stand-alone runnable JAR file. The JAR file for each release is
published [here](http://dev.opentripplanner.org/jars/). In addition, whenever new changes are made to the master branch 
of the main OTP repository on Github, the resulting JAR is published to the
 [same location](http://dev.opentripplanner.org/jars/otp-latest-master.jar).


## Building from Source

You may also choose to build OTP from its source code. If you will be modifying OTP you will need to know how to rebuild
 it (though your IDE may take care of this build cycle for you). If you have the right software installed, building OTP from source is not particularly difficult. You should only need Git (our version control system), a Java Development Kit (JDK), Maven (our build system), and an internet connection so Maven can fetch all of OTP's dependencies (the libraries it needs to function).
On Ubuntu or Debian Linux you would install these packages with the following command:

(Move rest of text from Building-OTP wiki page.)


## Maven Repository

OpenTripPlanner is a Maven project. Maven is a combined build and dependency management system: it fetches
all the external libraries that OTP uses, runs all the commands to compile the OTP source code into runnable form,
performs tests, and can then deploy the final "artifact" (the runnable JAR file) to our Maven repository, from which it
can be automatically included in other Java projects.

This repository is machine-readable (by Maven or other build systems) and for the moment does not include any human readable indexes. 
You can nonetheless fetch an OTP JAR from this repository by constructing the proper URL for the release
you want. For example, release 0.13.0 will be found at `http://maven.conveyal.com/org/opentripplanner/otp/0.13.0/otp-0.13.0.jar`

To make use of OTP in another project, you must first specify our Maven repository in the Project Object Model (POM):

```XML
<repositories>
  <repository>
    <id>Conveyal</id>
    <name>Conveyal Maven Repository</name>
    <url>http://maven.conveyal.com/</url> 
  </repository>
</repositories>
```

And then specify OpenTripPlanner as a dependency:

```XML
<dependency>
  <groupId>org.opentripplanner</groupId>
  <artifactId>otp</artifactId>
  <version>0.13.0</version>
</dependency>
```

After each successful build, our continuous integration (CI) server deploys the final OTP "artifact" (the runnable JAR) 
to our Maven repository as a "SNAPSHOT" build. This means that a Maven project depending on OTP as a library can 
always fetch the latest work in progress by specifying the following artifact:
 
 ```XML
<dependency>
  <groupId>org.opentripplanner</groupId>
  <artifactId>otp</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
 ```
 
We may eventually migrate to the Gradle build system, but Gradle uses the same dependency management and 
repository system as Maven.


## Continuous integration

This section does not belong here, but can be moved later.
The OpenTripPlanner project has a continuous integration (CI) server at http://ci.opentripplanner.org. Any time a change
is pushed to the main OpenTripPlanner repository on GitHub, this server will compile and test the new code, providing
feedback on the stability of the build. It is also configured to run a battery of speed tests so that we can track
improvements due to optimizations and spot drops in performance as an unintended consequence of changes.

