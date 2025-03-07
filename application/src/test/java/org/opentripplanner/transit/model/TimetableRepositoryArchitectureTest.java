package org.opentripplanner.transit.model;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.opentripplanner.OtpArchitectureModules.FRAMEWORK;
import static org.opentripplanner.OtpArchitectureModules.FRAMEWORK_UTILS;
import static org.opentripplanner.OtpArchitectureModules.GEO_UTILS;
import static org.opentripplanner.OtpArchitectureModules.JACKSON_ANNOTATIONS;
import static org.opentripplanner.OtpArchitectureModules.OTP_ROOT;
import static org.opentripplanner.OtpArchitectureModules.RAPTOR_ADAPTER_API;
import static org.opentripplanner.OtpArchitectureModules.RAPTOR_API;
import static org.opentripplanner.OtpArchitectureModules.TRANSIT_MODEL;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.arch.ArchComponent;
import org.opentripplanner._support.arch.Package;

public class TimetableRepositoryArchitectureTest {

  private static final Package TRANSIT_FRAMEWORK = TRANSIT_MODEL.subPackage("framework");
  private static final Package BASIC = TRANSIT_MODEL.subPackage("basic");
  private static final Package ORGANIZATION = TRANSIT_MODEL.subPackage("organization");
  private static final Package NETWORK = TRANSIT_MODEL.subPackage("network");
  private static final Package SITE = TRANSIT_MODEL.subPackage("site");
  private static final Package TIMETABLE = TRANSIT_MODEL.subPackage("timetable");
  private static final Package TIMETABLE_BOOKING = TIMETABLE.subPackage("booking");
  private static final Package LEGACY_MODEL = OTP_ROOT.subPackage("model");

  @Test
  void enforceFrameworkPackageDependencies() {
    TRANSIT_FRAMEWORK.dependsOn(FRAMEWORK_UTILS).verify();
  }

  @Test
  void enforceBasicPackageDependencies() {
    var resources = FRAMEWORK.subPackage("resources");
    BASIC.dependsOn(FRAMEWORK_UTILS, GEO_UTILS, resources, TRANSIT_FRAMEWORK).verify();
  }

  @Test
  void enforceOrganizationPackageDependencies() {
    ORGANIZATION.dependsOn(FRAMEWORK_UTILS, TRANSIT_FRAMEWORK, BASIC).verify();
  }

  @Test
  void enforceSitePackageDependencies() {
    SITE.dependsOn(
      FRAMEWORK_UTILS,
      JACKSON_ANNOTATIONS,
      GEO_UTILS,
      TRANSIT_FRAMEWORK,
      BASIC,
      ORGANIZATION
    ).verify();
  }

  @Test
  void enforceNetworkPackageDependencies() {
    // TODO OTP2 temporarily allow circular dependency between network and timetable
    NETWORK.dependsOn(
      FRAMEWORK_UTILS,
      GEO_UTILS,
      TRANSIT_FRAMEWORK,
      BASIC,
      ORGANIZATION,
      SITE,
      TIMETABLE,
      LEGACY_MODEL,
      RAPTOR_API,
      RAPTOR_ADAPTER_API
    ).verify();
  }

  @Test
  void enforceTimetablePackageDependencies() {
    TIMETABLE.dependsOn(
      FRAMEWORK_UTILS,
      TRANSIT_FRAMEWORK,
      BASIC,
      ORGANIZATION,
      NETWORK,
      SITE,
      TIMETABLE_BOOKING,
      LEGACY_MODEL
    ).verify();
  }

  @Test
  // TODO OTP2 temporarily allow circular dependency between network and timetable
  @Disabled
  void enforceNoCyclicDependencies() {
    slices()
      .matching(TRANSIT_MODEL.packageIdentifierAllSubPackages())
      .should()
      .beFreeOfCycles()
      .check(ArchComponent.OTP_CLASSES);
  }
}
