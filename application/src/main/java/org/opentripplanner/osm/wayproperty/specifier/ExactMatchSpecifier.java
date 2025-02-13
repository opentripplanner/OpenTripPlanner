package org.opentripplanner.osm.wayproperty.specifier;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.osm.model.OsmEntity;

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
 * @see org.opentripplanner.osm.tagmapping.HoustonMapper
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
    this(OsmSpecifier.parseConditions(spec, ";"));
  }

  public ExactMatchSpecifier(Condition... conditions) {
    this.conditions = Arrays.asList(conditions);
    bestMatchScore = this.conditions.size() * MATCH_MULTIPLIER;
  }

  @Override
  public Scores matchScores(OsmEntity way) {
    return new Scores(
      allForwardTagsMatch(way) ? bestMatchScore : NO_MATCH_SCORE,
      allBackwardTagsMatch(way) ? bestMatchScore : NO_MATCH_SCORE
    );
  }

  @Override
  public int matchScore(OsmEntity way) {
    if (allTagsMatch(way)) {
      return bestMatchScore;
    } else {
      return NO_MATCH_SCORE;
    }
  }

  @Override
  public String toDocString() {
    return conditions.stream().map(Object::toString).collect(Collectors.joining("; "));
  }

  public boolean allTagsMatch(OsmEntity way) {
    return conditions.stream().allMatch(o -> o.isMatch(way));
  }

  public boolean allBackwardTagsMatch(OsmEntity way) {
    return conditions.stream().allMatch(c -> c.isBackwardMatch(way));
  }

  public boolean allForwardTagsMatch(OsmEntity way) {
    return conditions.stream().allMatch(c -> c.isForwardMatch(way));
  }

  public static ExactMatchSpecifier exact(String spec) {
    return new ExactMatchSpecifier(spec);
  }
}
