package org.opentripplanner.graph_builder.module.osm.specifier;

import java.util.Arrays;
import java.util.List;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public interface OsmSpecifier {
  static boolean matchesWildcard(String value, String matchValue) {
    return matchValue != null && value != null && value.equals("*");
  }

  static List<Tag> getPairsFromString(String spec, String separator) {
    return Arrays
      .stream(spec.split(separator))
      .filter(p -> !p.isEmpty())
      .map(pair -> {
        var kv = pair.split("=");
        return new Tag(kv[0].toLowerCase(), kv[1].toLowerCase());
      })
      .toList();
  }

  Scores matchScores(OSMWithTags match);

  int matchScore(OSMWithTags match);

  record Tag(String key, String value) {}

  record Scores(int left, int right) {}
}
