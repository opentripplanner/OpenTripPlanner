package org.opentripplanner.graph_builder.module.osm.specifier;

import java.util.List;
import java.util.Objects;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public class ExactMatchSpecifier implements OsmSpecifier {

  private final List<Tag> pairs;

  public ExactMatchSpecifier(String spec) {
    this.pairs = OsmSpecifier.getTagsFromString(spec, ";");
  }

  @Override
  public Scores matchScores(OSMWithTags way) {
    var allTagsMatch = pairs.stream().allMatch(p -> matchValue(way.getTag(p.key()), p.value()));
    if (allTagsMatch) {
      return new Scores(Integer.MAX_VALUE, Integer.MAX_VALUE);
    } else {
      return new Scores(Integer.MIN_VALUE, Integer.MIN_VALUE);
    }
  }

  @Override
  public int matchScore(OSMWithTags match) {
    return 0;
  }

  private static boolean matchValue(String value, String matchValue) {
    return (
      (Objects.nonNull(value) && value.equals(matchValue)) ||
      OsmSpecifier.matchesWildcard(value, matchValue)
    );
  }
}
