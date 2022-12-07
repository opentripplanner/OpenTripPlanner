package org.opentripplanner.openstreetmap.wayproperty.specifier;

import java.util.Arrays;
import java.util.List;
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
 * @see org.opentripplanner.openstreetmap.tagmapping.HoustonMapper
 */
public class ExactMatchSpecifier implements OsmSpecifier {

  /**
   * If there is an exact match then the number of pairs are multiplied with this number.
   * <p>
   * Must be higher than {@link BestMatchSpecifier#EXACT_MATCH_SCORE}.
   */
  public static final int MATCH_MULTIPLIER = 200;
  public static final int NO_MATCH_SCORE = 0;
  private final List<Condition> conditions;
  private final int bestMatchScore;

  public ExactMatchSpecifier(String spec) {
    this(OsmSpecifier.parseEqualsTests(spec, ";"));
  }

  public ExactMatchSpecifier(Condition... conditions) {
    this.conditions = Arrays.asList(conditions);
    if (this.conditions.stream().anyMatch(Condition::isWildcard)) {
      throw new IllegalArgumentException(
        "Wildcards are not allowed in %s".formatted(this.getClass().getSimpleName())
      );
    }
    bestMatchScore = this.conditions.size() * MATCH_MULTIPLIER;
  }

  @Override
  public Scores matchScores(OSMWithTags way) {
    return Scores.of(matchScore(way));
  }

  @Override
  public int matchScore(OSMWithTags way) {
    if (allTagsMatch(way)) {
      return bestMatchScore;
    } else {
      return NO_MATCH_SCORE;
    }
  }

  public boolean allTagsMatch(OSMWithTags way) {
    return conditions.stream().allMatch(o -> o.matches(way));
  }

  public static ExactMatchSpecifier exact(String spec) {
    return new ExactMatchSpecifier(spec);
  }
}
