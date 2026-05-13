package org.opentripplanner.apis;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.Test;
import org.opentripplanner._support.arch.ArchComponent;
import org.opentripplanner._support.arch.Package;

/**
 * Enforces that the GraphQL API implementations remain isolated from each other.
 * Cross-imports lead to runtime ClassCastExceptions because each API installs its
 * own request-context type into the GraphQL execution context.
 */
public class ApisArchitectureTest {

  private static final Package APIS = Package.of("org.opentripplanner.apis");
  private static final Package GTFS = APIS.subPackage("gtfs..");
  private static final Package TRANSMODEL = APIS.subPackage("transmodel..");

  @Test
  void transmodelMustNotDependOnGtfs() {
    noClasses()
      .that()
      .resideInAPackage(TRANSMODEL.packageIdentifier())
      .should()
      .dependOnClassesThat()
      .resideInAPackage(GTFS.packageIdentifier())
      .check(ArchComponent.OTP_CLASSES);
  }

  @Test
  void gtfsMustNotDependOnTransmodel() {
    noClasses()
      .that()
      .resideInAPackage(GTFS.packageIdentifier())
      .should()
      .dependOnClassesThat()
      .resideInAPackage(TRANSMODEL.packageIdentifier())
      .check(ArchComponent.OTP_CLASSES);
  }
}
