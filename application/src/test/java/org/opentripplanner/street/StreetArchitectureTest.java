package org.opentripplanner.street;

import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.arch.Package;

@Disabled
public class StreetArchitectureTest {

  private static final Package STREET = Package.of("org.opentripplanner.street..");
  private static final Package[] ALLOWED_DEPS = Stream.of(
    "org.opentripplanner.astar..",
    "org.opentripplanner.transit.model.basic",
    "org.opentripplanner.transit.model.framework",
    "org.opentripplanner.utils..",
    "org.opentripplanner.framework..",
    "org.locationtech.jts..",
    "org.geotools..",
    "dagger.."
  )
    .map(Package::of)
    .toArray(Package[]::new);

  @Test
  void enforceStreetPackageIsolation() {
    STREET.dependsOn(ALLOWED_DEPS).verify();
  }
}
