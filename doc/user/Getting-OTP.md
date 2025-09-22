# Getting OpenTripPlanner

## Pre-built JARs

OpenTripPlanner is distributed as a single stand-alone runnable JAR file. We create a tag and
release page on GitHub for each release version, and also deploy them to the Maven Central
repository. You can go to
the [release pages on GitHub](https://github.com/opentripplanner/OpenTripPlanner/releases)
or [the OTP directory at Maven Central](https://repo1.maven.org/maven2/org/opentripplanner/otp-shaded/),
navigate to the highest version number, and download the jar file `otp-shaded-VERSION.jar`.

Note that version numbers like `v2.1.0-rc1` or `v2.8.1-SNAPSHOT` refer to development builds _
before_ the release version `v2.8.1`. The existence of a build `vX.Y.Z-SNAPSHOT` does not mean
that `vX.Y.Z` has been released yet.

We use the [Github Actions CI system](https://github.com/opentripplanner/OpenTripPlanner/actions) to
build OTP every time a change is made. You can find the JARs resulting from those builds in
the [Github Packages repository](https://github.com/opentripplanner/OpenTripPlanner/packages/562174)
. It can be harder to find the specific version you're looking for here, so we recommend using the
release pages or Maven Central as described above.

## Building from Source

You may also choose to build OTP from its source code. If you will be modifying OTP you will need to
know how to rebuild it (though your IDE may take care of this build cycle for you). If you have the
right software installed, building OTP locally from its source code is not particularly difficult.
You should only need the following software:

- Git, a version control system

- Java Development Kit, preferably version 21

- Maven, a build and dependency management system

You will also need a reliable internet connection so Maven can fetch all of OTP's dependencies (the
libraries it uses).

Once you have these packages installed, create and/or switch to the directory where you will keep
your Git repositories and make a local copy of the OTP source code:

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

Maven should then be able to download all the libraries and other dependencies necessary to compile
OTP. If all goes well you should see a success message like the following:

```
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 42.164s
[INFO] Finished at: Tue Feb 18 19:35:48 CET 2014
[INFO] Final Memory: 88M/695M
[INFO] ------------------------------------------------------------------------
```

This build process should produce a JAR file called `otp-shaded-x.y.z.jar` in the
`otp-shaded/target/` directory which contains all the compiled OTP classes and their dependencies
(the external libraries they use). The shell script called 'otp' in the root of the cloned repository
will start the main class of that JAR file under a Java virtual machine, so after the Maven build
completes you should be able to run `./otp --help` and see an OTP help message including command line
options. Due to the way Maven works, this script is not executable by default, so you will need to do
`chmod u+x ./otp` before you run it to mark it as executable.

The words "clean package" are the build steps you want to run. You're telling maven to clean up any
extraneous junk in the directory, then perform all the build steps, including compilation, up to and
including "package", which bundles the compiled program into a single JAR file for distribution.

If you have just cloned OTP you will be working with the default "master" branch, where most active
development occurs. This is not the most stable or deployment-ready code available. To avoid newly
minted bugs or undocumented behavior, you can use Git to check out a specific release (tag or
branch) of OTP to work with. The Maven build also includes many time-consuming integration tests.
When working with a stable release of OTP, you may want to turn them off by adding the
switch: `-DskipTests`.

For example, you could do the following:

```bash
cd OpenTripPlanner
git checkout v2.8.1
git clean -df
mvn clean package -DskipTests
```

Please note that the build process multiple jar files. The `otp-shaded-VERSION.jar` is much bigger
because it contains copies of all the external libraries that OTP uses. It serves as a stand-alone
runnable distribution of OTP. The other jar files are regular Java jar files. These JAR files are 
useful when OTP is included as a component in some other project, where we want the dependency
management system to gather all the external libraries automatically.

## Maven Repository

OpenTripPlanner is a Maven project. Maven is a combined build and dependency management system: it
fetches all the external libraries that OTP uses, runs all the commands to compile the OTP source
code into runnable form, performs tests, and can then deploy the final "artifact" (the runnable JAR
file) to the Maven repository, from which it can be automatically included in other Java projects.

This repository is machine-readable (by Maven or other build systems) and also provides human
readable directory listings via HTTP. You can fetch an OTP JAR from this repository by constructing
the proper URL for the release you want. For example, release 2.8.1 will be found
at `https://repo1.maven.org/maven2/org/opentripplanner/otp-shaded/2.8.1/otp-shaded-2.8.1-shaded.jar`.

To make use of OTP in another Maven project, you must specify it as a dependency in that
project's `pom.xml`:

```XML
<dependency>
  <groupId>org.opentripplanner</groupId>
  <artifactId>otp</artifactId>
  <version>2.8.1</version>
</dependency>
```
