package org.opentripplanner.graph_builder.module.osm.specifier;

import java.util.List;
import java.util.Objects;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

/**
 * This specifier allows you to specify a very precise match. It will only result in a positive when
 * _all_ key/value pairs match exactly.
 * <p>
 * It's useful when you want to have a long, very specific specifier that should only match a very
 * limited number of ways.
 * <p>
 * If you'd use a {@link BestMatchSpecifier} then the likelihood of the long spec matching unwanted
 * ways would be high.
 *
 * @see org.opentripplanner.graph_builder.module.osm.HoustonWayPropertySetSource
 */
public class ExactMatchSpecifier implements OsmSpecifier {

  private final List<Tag> pairs;

  public ExactMatchSpecifier(String spec) {
    this.pairs = OsmSpecifier.getTagsFromString(spec, ";");
    if (this.pairs.stream().anyMatch(Tag::isWildcard)) {
      throw new RuntimeException(
        "Wildcards are not allowed in %s".formatted(this.getClass().getSimpleName())
      );
    }
  }

  @Override
  public Scores matchScores(OSMWithTags way) {
    var allTagsMatch = matchesExactly(way);
    if (allTagsMatch) {
      return new Scores(Integer.MAX_VALUE, Integer.MAX_VALUE);
    } else {
      return new Scores(Integer.MIN_VALUE, Integer.MIN_VALUE);
    }
  }

  @Override
  public int matchScore(OSMWithTags way) {
    if (matchesExactly(way)) {
      return Integer.MAX_VALUE;
    } else {
      return Integer.MIN_VALUE;
    }
  }

  public boolean matchesExactly(OSMWithTags way) {
    return pairs.stream().allMatch(p -> matchValue(way.getTag(p.key()), p.value()));
  }

  private static boolean matchValue(String wayValue, String specValue) {
    return (
      (Objects.nonNull(wayValue) && wayValue.equals(specValue)) ||
      OsmSpecifier.matchesWildcard(wayValue, specValue)
    );
  }
}
