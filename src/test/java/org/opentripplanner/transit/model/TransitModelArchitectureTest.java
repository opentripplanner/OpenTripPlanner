package org.opentripplanner.transit.model;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.opentripplanner.OtpArchitectureModules.TRANSIT_MODEL;
import static org.opentripplanner.OtpArchitectureModules.UTILS;

import org.junit.jupiter.api.Test;
import org.opentripplanner._support.arch.ArchComponent;
import org.opentripplanner._support.arch.Package;

public class TransitModelArchitectureTest {

  private static final Package BASIC = TRANSIT_MODEL.subPackage("basic");
  private static final Package ORGANIZATION = TRANSIT_MODEL.subPackage("organization");

  @Test
  void enforcePackageDependencies() {
    BASIC.dependsOn(UTILS).verify();
    ORGANIZATION.dependsOn(BASIC, UTILS).verify();
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
