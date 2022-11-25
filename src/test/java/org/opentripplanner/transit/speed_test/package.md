# SpeedTest

This package contains the MANUAL SpeedTest used to performance tune the Raptor algorithm. Code
inside this package should not be used outside this package.

When changing the core logic of Raptor this test can be used to detect changes in the result and
performance.

To run the SpeedTest use the {@code --help} option to se the documentation. There is not much
documentation on this tool, hopefully with time, we will add more doc and maybe automate part of
this test.

Example input files and setup is included in the resource test folder:

- {@code test/ci-performance-test/}.

## Running

```
mvn compiler:testCompile exec:java -Dexec.mainClass="org.opentripplanner.transit.speed_test.SpeedTest" -Dexec.classpathScope=test -Dexec.args="--dir=test/ci-performance-test/ -p md -n 4"
```

## CI

The test is run after every merge to master. Its Github Actions workflow is defined
in [performance-test.yml](../../../../../../../../.github/workflows/performance-test.yml).