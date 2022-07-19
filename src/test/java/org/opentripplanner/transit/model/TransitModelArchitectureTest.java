package org.opentripplanner.transit.model;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.opentripplanner.OtpArchitectureModules.GEO_UTIL;
import static org.opentripplanner.OtpArchitectureModules.JACKSON_ANNOTATIONS;
import static org.opentripplanner.OtpArchitectureModules.JTS_GEOM;
import static org.opentripplanner.OtpArchitectureModules.TRANSIT_MODEL;
import static org.opentripplanner.OtpArchitectureModules.UTILS;

import org.junit.jupiter.api.Test;
import org.opentripplanner._support.arch.ArchComponent;
import org.opentripplanner._support.arch.Package;

public class TransitModelArchitectureTest {

  private static final Package FRAMEWORK = TRANSIT_MODEL.subPackage("framework");
  private static final Package BASIC = TRANSIT_MODEL.subPackage("basic");
  private static final Package ORGANIZATION = TRANSIT_MODEL.subPackage("organization");
  private static final Package NETWORK = TRANSIT_MODEL.subPackage("network");
  private static final Package SITE = TRANSIT_MODEL.subPackage("site");
  private static final Package TIMETABLE = TRANSIT_MODEL.subPackage("timetable");

  @Test
  void enforcePackageDependencies() {
    FRAMEWORK.dependsOn(UTILS).verify();
    BASIC.dependsOn(UTILS, JTS_GEOM).verify();
    ORGANIZATION.dependsOn(UTILS, FRAMEWORK, BASIC).verify();
    SITE
      .dependsOn(UTILS, JACKSON_ANNOTATIONS, JTS_GEOM, GEO_UTIL, FRAMEWORK, BASIC, ORGANIZATION)
      .verify();
    NETWORK.dependsOn(UTILS, FRAMEWORK, BASIC, ORGANIZATION, SITE).verify();
    TIMETABLE.dependsOn(UTILS, FRAMEWORK, BASIC, ORGANIZATION, NETWORK, SITE);
  }

  @Test
  void enforceNoCyclicDependencies() {
    slices()
      .matching("org.opentripplanner.transit.model.(*)..")
      .should()
      .beFreeOfCycles()
      .check(ArchComponent.OTP_CLASSES);
  }
}
