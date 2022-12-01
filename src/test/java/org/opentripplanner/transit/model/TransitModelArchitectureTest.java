package org.opentripplanner.transit.model;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.opentripplanner.OtpArchitectureModules.GEO_UTILS;
import static org.opentripplanner.OtpArchitectureModules.JACKSON_ANNOTATIONS;
import static org.opentripplanner.OtpArchitectureModules.OTP_ROOT;
import static org.opentripplanner.OtpArchitectureModules.RAPTOR_ADAPTER_API;
import static org.opentripplanner.OtpArchitectureModules.RAPTOR_API;
import static org.opentripplanner.OtpArchitectureModules.TRANSIT_MODEL;
import static org.opentripplanner.OtpArchitectureModules.UTIL;
import static org.opentripplanner.OtpArchitectureModules.UTILS;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.arch.ArchComponent;
import org.opentripplanner._support.arch.Package;

public class TransitModelArchitectureTest {

  private static final Package TRANSIT_FRAMEWORK = TRANSIT_MODEL.subPackage("framework");
  private static final Package BASIC = TRANSIT_MODEL.subPackage("basic");
  private static final Package ORGANIZATION = TRANSIT_MODEL.subPackage("organization");
  private static final Package NETWORK = TRANSIT_MODEL.subPackage("network");
  private static final Package SITE = TRANSIT_MODEL.subPackage("site");
  private static final Package TIMETABLE = TRANSIT_MODEL.subPackage("timetable");
  private static final Package LEGACY_MODEL = OTP_ROOT.subPackage("model");

  @Test
  void enforceFrameworkPackageDependencies() {
    TRANSIT_FRAMEWORK.dependsOn(UTILS).verify();
  }

  @Test
  void enforceBasicPackageDependencies() {
    // TODO Remove dependency to resources
    var resources = UTIL.subPackage("resources");
    BASIC.dependsOn(UTILS, GEO_UTILS, resources, TRANSIT_FRAMEWORK).verify();
  }

  @Test
  void enforceOrganizationPackageDependencies() {
    ORGANIZATION.dependsOn(UTILS, TRANSIT_FRAMEWORK, BASIC).verify();
  }

  @Test
  void enforceSitePackageDependencies() {
    SITE
      .dependsOn(UTILS, JACKSON_ANNOTATIONS, GEO_UTILS, TRANSIT_FRAMEWORK, BASIC, ORGANIZATION)
      .verify();
  }

  @Test
  void enforceNetworkPackageDependencies() {
    // TODO OTP2 temporarily allow circular dependency between network and timetable
    NETWORK
      .dependsOn(
        UTILS,
        GEO_UTILS,
        TRANSIT_FRAMEWORK,
        BASIC,
        ORGANIZATION,
        SITE,
        TIMETABLE,
        LEGACY_MODEL,
        RAPTOR_API,
        RAPTOR_ADAPTER_API
      )
      .verify();
  }

  @Test
  void enforceTimetablePackageDependencies() {
    TIMETABLE
      .dependsOn(UTILS, TRANSIT_FRAMEWORK, BASIC, ORGANIZATION, NETWORK, SITE, LEGACY_MODEL)
      .verify();
  }

  @Test
  // TODO OTP2 temporarily allow circular dependency between network and timetable
  @Disabled
  void enforceNoCyclicDependencies() {
    slices()
      .matching("org.opentripplanner.transit.model.(*)..")
      .should()
      .beFreeOfCycles()
      .check(ArchComponent.OTP_CLASSES);
  }
}
