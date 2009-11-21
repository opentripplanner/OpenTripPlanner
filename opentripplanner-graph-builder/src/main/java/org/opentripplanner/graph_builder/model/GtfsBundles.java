package org.opentripplanner.graph_builder.model;

import java.util.ArrayList;
import java.util.List;

public class GtfsBundles {
  private List<GtfsBundle> bundles = new ArrayList<GtfsBundle>();

  public List<GtfsBundle> getBundles() {
    return bundles;
  }

  public void setBundles(List<GtfsBundle> bundles) {
    this.bundles = bundles;
  }
}
