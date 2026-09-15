package org.opentripplanner.raptor._support.arch;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Module implements ArchComponent {

  private final List<Package> packages;

  private Module(List<Package> packages) {
    this.packages = packages;
  }

  public static Module of(ArchComponent... components) {
    return new Module(Arrays.stream(components).flatMap(c -> c.packages().stream()).toList());
  }

  @Override
  public Collection<Package> packages() {
    return packages;
  }
}
