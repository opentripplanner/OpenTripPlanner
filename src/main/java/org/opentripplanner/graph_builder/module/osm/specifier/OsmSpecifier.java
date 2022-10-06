package org.opentripplanner.graph_builder.module.osm.specifier;

import org.opentripplanner.openstreetmap.model.OSMWithTags;

public interface OsmSpecifier {
  Scores matchScores(OSMWithTags match);

  int matchScore(OSMWithTags match);

  record Tag(String key, String value) {}

  record Scores(int left, int right) {}
}
