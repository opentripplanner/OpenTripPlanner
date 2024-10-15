package org.opentripplanner.datastore;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.opentripplanner.OtpArchitectureModules.DAGGER;
import static org.opentripplanner.OtpArchitectureModules.DATASTORE;
import static org.opentripplanner.OtpArchitectureModules.FRAMEWORK;
import static org.opentripplanner.OtpArchitectureModules.FRAMEWORK_UTILS;
import static org.opentripplanner.OtpArchitectureModules.GOOGLE_COLLECTIONS;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.arch.ArchComponent;
import org.opentripplanner._support.arch.Package;

public class DataStoreArchitectureTest {

  private static final Package API = DATASTORE.subPackage("api");
  private static final Package BASE = DATASTORE.subPackage("base");
  private static final Package CONFIGURE = DATASTORE.subPackage("configure");
  private static final Package FILE = DATASTORE.subPackage("file");
  private static final Package HTTPS = DATASTORE.subPackage("https");
  private static final Package ZIP_FILE = DATASTORE.subPackage("file");
  private static final Package SERVICE = DATASTORE;

  private static final Package FRAMEWORK_IO = FRAMEWORK.subPackage("io");
  private static final Package APACHE_HTTP = Package.of("org.apache.hc..");

  @Test
  void enforceApiPackageDependencies() {
    API.dependsOn(FRAMEWORK_UTILS).verify();
  }

  @Test
  void enforceBasePackageDependencies() {
    BASE.dependsOn(API).verify();
  }

  @Test
  void enforceFilePackageDependencies() {
    FILE.dependsOn(API, BASE, FRAMEWORK_UTILS, FRAMEWORK_IO).verify();
  }

  @Test
  void enforceHttpsPackageDependencies() {
    HTTPS.dependsOn(API, BASE, ZIP_FILE, FRAMEWORK_UTILS, FRAMEWORK_IO, APACHE_HTTP).verify();
  }

  @Test
  @Disabled
  void enforceZipFilePackageDependencies() {
    ZIP_FILE.verify();
  }

  @Test
  void enforceServicePackageDependencies() {
    SERVICE.dependsOn(API, BASE, GOOGLE_COLLECTIONS).verify();
  }

  @Test
  void enforceConfigureDependencies() {
    CONFIGURE.dependsOn(API, BASE, FILE, HTTPS, SERVICE, DAGGER).verify();
  }

  @Test
  void enforceNoCyclicDependencies() {
    slices()
      .matching(DATASTORE.packageIdentifierAllSubPackages())
      .should()
      .beFreeOfCycles()
      .check(ArchComponent.OTP_CLASSES);
  }
}
