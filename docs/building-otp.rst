========================
Building OTP from source
========================

To build OTP locally from source you will need a few pieces of software:
- Java Development Kit, preferably version 7 (AKA 1.7)
- Git, a version control system
- Maven, a build and dependency management system

To install these packages on a Debian/Ubuntu system, run: `sudo apt-get install openjdk-7-jdk maven git`.

Then create and/or switch to the directory where you keep your git repositories::

    mkdir git
    cd git
    git clone git@github.com:opentripplanner/OpenTripPlanner.git

Then change to the newly cloned OpenTripPlanner repository directory and run a build::

    cd OpenTripPlanner
    mvn clean package

Alternatively you can check out a specific tag or branch of OTP to work with. The Maven build also includes many time-consuming integration tests, and you may want to turn them off by adding a switch: `mvn clean package -DskipTests`::

    cd OpenTripPlanner
    git checkout 1.0.0_test
    mvn clean package -DskipTests

Maven should then be able to download all the libraries and other dependencies necessary to compile OTP. If all goes well you should see a success message like the following::

    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    [INFO] Total time: 42.164s
    [INFO] Finished at: Tue Feb 18 19:35:48 CET 2014
    [INFO] Final Memory: 88M/695M
    [INFO] ------------------------------------------------------------------------

This build process should produce a JAR file called `otp.jar` in the `otp-core/target/` directory which contains all the compiled OTP classes and their dependencies. The shell script called 'otp' in the root of the cloned repository will start the main class of that JAR file under a Java virtual machine, so after the Maven build completes you should be able to run `./otp --help` and see an OTP help message including command line options.
