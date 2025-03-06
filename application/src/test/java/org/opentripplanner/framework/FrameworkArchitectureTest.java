package org.opentripplanner.framework;

import static org.opentripplanner.OtpArchitectureModules.FRAMEWORK;
import static org.opentripplanner.OtpArchitectureModules.GEO_JSON;
import static org.opentripplanner.OtpArchitectureModules.GEO_TOOLS;
import static org.opentripplanner.OtpArchitectureModules.GNU_TROVE;
import static org.opentripplanner.OtpArchitectureModules.JTS_GEOM;
import static org.opentripplanner.OtpArchitectureModules.OPEN_GIS;
import static org.opentripplanner.OtpArchitectureModules.OTP_UTILS;

import org.junit.jupiter.api.Test;
import org.opentripplanner._support.arch.Module;
import org.opentripplanner._support.arch.Package;

public class FrameworkArchitectureTest {

  private static final Package APACHE_HTTP = Package.of("org.apache.hc..");
  private static final Package GUAVA_COLLECTIONS = Package.of("com.google.common.collect");

  private static final Module XML_MODULES = Module.of(
    Package.of("com.fasterxml.jackson.."),
    Package.of("org.w3c.dom"),
    Package.of("org.xml.sax")
  );
  private static final Package APPLICATION = FRAMEWORK.subPackage("application");
  private static final Package COLLECTION = FRAMEWORK.subPackage("collection");
  private static final Package FUNCTIONAL = FRAMEWORK.subPackage("functional");
  private static final Package GEOMETRY = FRAMEWORK.subPackage("geometry");
  private static final Package I18N = FRAMEWORK.subPackage("i18n");
  private static final Package IO = FRAMEWORK.subPackage("io");
  private static final Package LOGGING = FRAMEWORK.subPackage("logging");
  private static final Package RESOURCES = FRAMEWORK.subPackage("resources");
  private static final Package TIME = FRAMEWORK.subPackage("time");

  @Test
  void enforceApplicationPackageDependencies() {
    APPLICATION.dependsOn(OTP_UTILS).verify();
  }

  @Test
  void enforceCollectionPackageDependencies() {
    COLLECTION.dependsOn(GNU_TROVE).verify();
  }

  @Test
  void enforceFunctionalPackageDependencies() {
    FUNCTIONAL.verify();
  }

  @Test
  void enforceGeometryPackageDependencies() {
    GEOMETRY.dependsOn(
      GEO_JSON,
      GEO_TOOLS,
      GNU_TROVE,
      JTS_GEOM,
      OPEN_GIS,
      GUAVA_COLLECTIONS,
      OTP_UTILS
    ).verify();
  }

  @Test
  void enforceI18nPackageDependencies() {
    I18N.dependsOn(RESOURCES).verify();
  }

  @Test
  void enforceIoPackageDependencies() {
    IO.dependsOn(APACHE_HTTP, XML_MODULES).verify();
  }

  @Test
  void enforceLoggingPackageDependencies() {
    LOGGING.dependsOn(OTP_UTILS).verify();
  }

  @Test
  void enforceResourcesPackageDependencies() {
    RESOURCES.verify();
  }

  @Test
  void enforceTimePackageDependencies() {
    TIME.dependsOn(OTP_UTILS).verify();
  }
}
