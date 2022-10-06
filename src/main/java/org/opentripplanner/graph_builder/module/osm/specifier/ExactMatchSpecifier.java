package org.opentripplanner.graph_builder.module.osm.specifier;

import org.opentripplanner.openstreetmap.model.OSMWithTags;

public class ExactMatchSpecifier implements OsmSpecifier {

  @Override
  public Scores matchScores(OSMWithTags match) {
    return null;
  }

  @Override
  public int matchScore(OSMWithTags match) {
    return 0;
  }
}
