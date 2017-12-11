# Getting OpenTripPlanner

## Pre-built JARs

OpenTripPlanner is distributed as a single stand-alone runnable JAR file. These JARs are deployed to the Sonatype OSSRH Maven repository, and release versions are synced to the Maven Central repository. Most people will want to go to [the OTP directory at Maven Central](https://repo1.maven.org/maven2/org/opentripplanner/otp/), navigate to the directory for the highest version number, and download the file whose name ends with `.shaded.jar`.

We use the [Travis continuous integration system](https://travis-ci.org/opentripplanner/OpenTripPlanner) to build OTP every time a change is made. You can find the JARs resulting from those builds in the [OSSRH staging repository](https://oss.sonatype.org/content/repositories/staging/org/opentripplanner/otp/). A directory named `x.y.z-SNAPSHOT` contain JARs for builds leading up to (preceding) the `x.y.z` release. Files within `SNAPSHOT` directories are named using the date and time that the build occurred.

## Building from Source

You may also choose to build OTP from its source code. If you will be modifying OTP you will need to know how to rebuild
 it (though your IDE may take care of this build cycle for you). If you have the right software installed, 
 building OTP locally from its source code is not particularly difficult. You should only need the following software:

- Git, a version control system

- Java Development Kit, preferably version 8 (AKA version 1.8)

- Maven, a build and dependency management system

You will also need a reliable internet connection so Maven can fetch all of OTP's dependencies (the libraries it uses). 
To install these software packages on a Debian or Ubuntu system, run:

    sudo apt-get install openjdk-8-jdk maven git

Once you have these packages installed, create and/or switch to the directory where you will keep your Git repositories and make a local copy of the OTP source code:

```shell
mkdir git
cd git
git clone git@github.com:opentripplanner/OpenTripPlanner.git
```

Then change to the newly cloned OpenTripPlanner repository directory and start a build:

```shell
cd OpenTripPlanner
mvn clean package
```
Maven should then be able to download all the libraries and other dependencies necessary to compile OTP. 
If all goes well you should see a success message like the following:

```
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 42.164s
[INFO] Finished at: Tue Feb 18 19:35:48 CET 2014
[INFO] Final Memory: 88M/695M
[INFO] ------------------------------------------------------------------------
```

This build process should produce a JAR file called `otp-x.y.z-shaded.jar` in the `target/` directory which contains
all the compiled OTP classes and their dependencies (the external libraries they use). The shell script called 'otp'
in the root of the cloned repository will
start the main class of that JAR file under a Java virtual machine, so after the Maven build completes you should be 
able to run `./otp --help` and see an OTP help message including command line options. Due to the way Maven works, this
script is not executable by default, so you will need to do `chmod u+x ./otp` before you run it to mark it as executable.

The words "clean package" are the build steps you want to run. You're telling maven to clean up any extraneous junk in
 the directory, then perform all the build steps, including compilation, up to and including "package",
 which bundles the compiled program into a single JAR file for distribution.
 
If you have just cloned OTP you will be working with the default "master" branch, where most active development occurs.
 This is not the most stable or deployment-ready code available. To avoid newly minted bugs or undocumented behavior,
 you can use Git to check out a specific release (tag or branch) of OTP to work with. The Maven build also includes 
 many time-consuming integration tests. When working with a stable release of OTP, 
 you may want to turn them off by adding the switch: `-DskipTests`.

For example, you could do the following:

```bash
cd OpenTripPlanner
git checkout opentripplanner-0.18.0
git clean -df
mvn clean package -DskipTests
```

Please note that the build process creates two distinct versions of the OTP JAR file. The one ending in `-shaded.jar`
is much bigger because it contains copies of all the external libraries that OTP uses.
It serves as a stand-alone runnable distribution of OTP. The one with a version number but without the word `shaded`
contains only OTP itself, without any external dependencies. This JAR is useful when OTP is included as a component in
some other project, where we want the dependency management system to gather all the external libraries automatically.


## Maven Repository

OpenTripPlanner is a Maven project. Maven is a combined build and dependency management system: it fetches
all the external libraries that OTP uses, runs all the commands to compile the OTP source code into runnable form,
performs tests, and can then deploy the final "artifact" (the runnable JAR file) to the Maven repository, from which it
can be automatically included in other Java projects.

This repository is machine-readable (by Maven or other build systems) and also provides human readable directory listings via HTTP. You can fetch an OTP JAR from this repository by constructing the proper URL for the release
you want. For example, release 1.1.0 will be found at `https://repo1.maven.org/maven2/org/opentripplanner/otp/1.1.0/otp-1.1.0-shaded.jar`.

To make use of OTP in another Maven project, you must specify it as a dependency:

```XML
<dependency>
  <groupId>org.opentripplanner</groupId>
  <artifactId>otp</artifactId>
  <version>1.2.0</version>
</dependency>
```

After each successful build, the [Travis continuous integration system](https://travis-ci.org/opentripplanner/OpenTripPlanner) deploys the final OTP "artifact" (the runnable JAR) to our Maven repository as a "SNAPSHOT" build. This means that a Maven project depending on OTP as a library can always fetch the latest work in progress by specifying a snapshot artifact:

 ```XML
 <repositories>
   <repository>
     <id>ossrh_snapshots</id>
     <name>Sonatype OSSRH Shapshot Repository</name>
     <url>https://oss.sonatype.org/content/repositories/snapshots/</url> 
   </repository>
 </repositories>
 ```

```XML
<dependency>
  <groupId>org.opentripplanner</groupId>
  <artifactId>otp</artifactId>
  <version>1.2.0-SNAPSHOT</version>
</dependency>
```
