package org.opentripplanner.graph_builder.module.osm.specifier;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public class ExactMatchSpecifier implements OsmSpecifier {

  @Override
  public P2<Integer> matchScores(OSMWithTags match) {
    return null;
  }

  @Override
  public int matchScore(OSMWithTags match) {
    return 0;
  }
}
