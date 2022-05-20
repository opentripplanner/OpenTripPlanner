package org.opentripplanner._support.arch;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Module implements ArchComponent {

  private final List<Package> packages;

  private Module(List<Package> packages) {
    this.packages = packages;
  }

  public static Module of(Package... packages) {
    return new Module(Arrays.asList(packages));
  }

  @Override
  public Collection<Package> packages() {
    return packages;
  }
}
