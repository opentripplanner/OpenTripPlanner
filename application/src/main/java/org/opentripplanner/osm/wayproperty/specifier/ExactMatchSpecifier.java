package org.opentripplanner.osm.wayproperty.specifier;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.TraverseDirection;

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
  private final Condition[] conditions;
  private final int bestMatchScore;

  public ExactMatchSpecifier(String spec) {
    this(OsmSpecifier.parseConditions(spec, ";"));
  }

  public ExactMatchSpecifier(Condition... conditions) {
    this.conditions = conditions;
    bestMatchScore = this.conditions.length * MATCH_MULTIPLIER;
  }

  @Override
  public int matchScore(OsmEntity way, TraverseDirection direction) {
    return allTagsMatch(way, direction) ? bestMatchScore : NO_MATCH_SCORE;
  }

  @Override
  public String toDocString() {
    return Arrays.stream(conditions).map(Object::toString).collect(Collectors.joining("; "));
  }

  public boolean allTagsMatch(OsmEntity way) {
    for (var c : conditions) {
      if (!c.isMatch(way)) {
        return false;
      }
    }
    return true;
  }

  public boolean allBackwardTagsMatch(OsmEntity way) {
    for (var c : conditions) {
      if (!c.isBackwardMatch(way)) {
        return false;
      }
    }
    return true;
  }

  public boolean allForwardTagsMatch(OsmEntity way) {
    for (var c : conditions) {
      if (!c.isForwardMatch(way)) {
        return false;
      }
    }
    return true;
  }

  private boolean allTagsMatch(OsmEntity way, TraverseDirection direction) {
    return switch (direction) {
      case DIRECTIONLESS -> allTagsMatch(way);
      case FORWARD -> allForwardTagsMatch(way);
      case BACKWARD -> allBackwardTagsMatch(way);
    };
  }

  public static ExactMatchSpecifier exact(String spec) {
    return new ExactMatchSpecifier(spec);
  }
}
