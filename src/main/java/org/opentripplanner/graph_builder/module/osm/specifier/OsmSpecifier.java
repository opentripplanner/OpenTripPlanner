package org.opentripplanner.graph_builder.module.osm.specifier;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public interface OsmSpecifier {
  P2<Integer> matchScores(OSMWithTags match);

  int matchScore(OSMWithTags match);

  record OsmTag(String key, String value) {}
}
