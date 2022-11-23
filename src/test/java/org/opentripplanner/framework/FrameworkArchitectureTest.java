package org.opentripplanner.framework;

import static org.opentripplanner.OtpArchitectureModules.FRAMEWORK;
import static org.opentripplanner.OtpArchitectureModules.UTIL;

import org.junit.jupiter.api.Test;
import org.opentripplanner._support.arch.Package;

public class FrameworkArchitectureTest {

  private static final Package COLLECTION = FRAMEWORK.subPackage("collection");
  private static final Package IO = FRAMEWORK.subPackage("io");
  private static final Package LANG = UTIL.subPackage("lang");
  private static final Package TEXT = FRAMEWORK.subPackage("text");
  private static final Package TIME = FRAMEWORK.subPackage("time");

  @Test
  void enforceCollectionPackageDependencies() {
    COLLECTION.verify();
  }

  @Test
  void enforceIoPackageDependencies() {
    IO.verify();
  }

  @Test
  void enforceTextPackageDependencies() {
    TEXT.dependsOn(LANG).verify();
  }

  @Test
  void enforceTimePackageDependencies() {
    TIME.verify();
  }
}
