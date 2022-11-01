package org.opentripplanner.util;

import static org.opentripplanner.OtpArchitectureModules.UTIL;

import org.junit.jupiter.api.Test;
import org.opentripplanner._support.arch.Package;

public class UtilArchitectureTest {

  private static final Package LANG = UTIL.subPackage("lang");
  private static final Package TIME = UTIL.subPackage("time");

  @Test
  void enforcePackageDependencies() {
    // The util packages needs cleanup, it contains model, framework and API classes. The strategy
    // is therefore to move the true util classes into sub packages, and then later to move the
    // reminding classes to the places they belong.

    // Utils should not have any dependencies
    TIME.verify();
    // It might sound strange that lang depend on time, but we allow this to avoid creating another
    // util package where we can put the ToStringBuilder classes(witch depend on time). As long as
    // we do not get cyclic dependencies between lang and time there is not problem with this.
    LANG.dependsOn(TIME).verify();
  }
}
