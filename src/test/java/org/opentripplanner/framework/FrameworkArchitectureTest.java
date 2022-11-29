package org.opentripplanner.framework;

import static org.opentripplanner.OtpArchitectureModules.FRAMEWORK;
import static org.opentripplanner.OtpArchitectureModules.GNU_TROVE;

import org.junit.jupiter.api.Test;
import org.opentripplanner._support.arch.Module;
import org.opentripplanner._support.arch.Package;

public class FrameworkArchitectureTest {

  private static final Package APACHE_HTTP = Package.of("org.apache.http..");

  private static final Module XML_MODULES = Module.of(
    Package.of("com.fasterxml.jackson.."),
    Package.of("org.w3c.dom"),
    Package.of("org.xml.sax")
  );
  private static final Package COLLECTION = FRAMEWORK.subPackage("collection");
  private static final Package IO = FRAMEWORK.subPackage("io");
  private static final Package LANG = FRAMEWORK.subPackage("lang");
  private static final Package LOGGING = FRAMEWORK.subPackage("logging");
  private static final Package TEXT = FRAMEWORK.subPackage("text");
  private static final Package TIME = FRAMEWORK.subPackage("time");
  private static final Package TO_STRING = FRAMEWORK.subPackage("tostring");

  @Test
  void enforceCollectionPackageDependencies() {
    COLLECTION.dependsOn(GNU_TROVE).verify();
  }

  @Test
  void enforceIoPackageDependencies() {
    IO.dependsOn(APACHE_HTTP, XML_MODULES).verify();
  }

  @Test
  void enforceLoggingPackageDependencies() {
    LOGGING.dependsOn(TEXT, TIME).verify();
  }

  @Test
  void enforceTextPackageDependencies() {
    TEXT.dependsOn(LANG).verify();
  }

  @Test
  void enforceTimePackageDependencies() {
    TIME.verify();
  }

  @Test
  void enforceToStingPackageDependencies() {
    TO_STRING.dependsOn(LANG, TIME).verify();
  }
}
