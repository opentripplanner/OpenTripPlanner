package org.opentripplanner.transit.model;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.opentripplanner.OtpArchitectureModules.TRANSIT_MODEL;
import static org.opentripplanner.OtpArchitectureModules.UTILS;

import org.junit.jupiter.api.Test;
import org.opentripplanner._support.arch.ArchComponent;
import org.opentripplanner._support.arch.Package;

public class TransitModelArchitectureTest {

  private static final Package FRAMEWORK = TRANSIT_MODEL.subPackage("framework");
  private static final Package ORGANIZATION = TRANSIT_MODEL.subPackage("organization");
  private static final Package NETWORK = TRANSIT_MODEL.subPackage("network");

  @Test
  void enforcePackageDependencies() {
    FRAMEWORK.dependsOn(UTILS).verify();
    ORGANIZATION.dependsOn(UTILS, FRAMEWORK).verify();
    NETWORK.dependsOn(UTILS, FRAMEWORK, ORGANIZATION).verify();
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
