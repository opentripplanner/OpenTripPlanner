package org.opentripplanner.osm.wayproperty.specifier;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Specifies a class of OSM tagged entities (e.g. ways) by a list of tags and their values (which
 * may be wildcards). The OSMSpecifier which matches the most tags on an OSM entity will win. In the
 * event that several OSMSpecifiers match the same number of tags, the one that does so using fewer
 * wildcards will win. For example, if one OSMSpecifier has the tags (highway=residential,
 * cycleway=*) and another has (highway=residential, surface=paved) and a way has the tags
 * (highway=residential, cycleway=lane, surface=paved) the second specifier will be applied to that
 * way (2 exact matches beats 1 exact match and a wildcard match).
 */
public class BestMatchSpecifier implements OsmSpecifier {

  /**
   * If a tag matches completely (both key and value match) this is the score returned for it.
   *
   * @see ExactMatchSpecifier#MATCH_MULTIPLIER
   */
  public static final int EXACT_MATCH_SCORE = 100;
  public static final int WILDCARD_MATCH_SCORE = 1;
  public static final int NO_MATCH_SCORE = 0;
  private final Condition[] conditions;

  /**
   * @deprecated Logic is fuzzy and unpredictable, use ExactMatchSpecifier instead
   */
  @Deprecated
  public BestMatchSpecifier(String spec) {
    conditions = OsmSpecifier.parseConditions(spec, ";");
  }

  @Override
  public Scores matchScores(OsmEntity way) {
    int backwardScore = 0, forwardScore = 0;
    int backwardMatches = 0, forwardMatches = 0;

    for (var test : conditions) {
      var forwardMatch = test.matchForward(way);
      var backwardMatch = test.matchBackward(way);

      int backwardTagScore = toTagScore(backwardMatch);
      backwardScore += backwardTagScore;
      if (backwardTagScore > 0) {
        backwardMatches++;
      }
      int forwardTagScore = toTagScore(forwardMatch);
      forwardScore += forwardTagScore;
      if (forwardTagScore > 0) {
        forwardMatches++;
      }
    }

    int allMatchBackwardBonus = (backwardMatches == conditions.length) ? 10 : 0;
    backwardScore += allMatchBackwardBonus;
    int allMatchForwardBonus = (forwardMatches == conditions.length) ? 10 : 0;
    forwardScore += allMatchForwardBonus;
    return new Scores(forwardScore, backwardScore);
  }

  @Override
  public int matchScore(OsmEntity way) {
    int score = 0;
    int matches = 0;
    for (var test : conditions) {
      var matchValue = test.match(way);
      int tagScore = toTagScore(matchValue);
      score += tagScore;
      if (tagScore > 0) {
        matches += 1;
      }
    }
    score += matches == conditions.length ? 10 : 0;
    return score;
  }

  @Override
  public String toDocString() {
    return Arrays.stream(conditions).map(Object::toString).collect(Collectors.joining("; "));
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass()).addObj("conditions", conditions).toString();
  }

  /**
   * Calculates a score indicating how well an OSM tag value matches the given matchValue. An exact
   * match is worth 100 points, a partial match on the part of the value before a colon is worth 75
   * points, and a wildcard match is worth only one point, to serve as a tiebreaker. A score of 0
   * means they do not match.
   */
  private static int toTagScore(Condition.MatchResult res) {
    return switch (res) {
      case EXACT -> EXACT_MATCH_SCORE;
      // wildcard matches are basically tiebreakers
      case WILDCARD -> WILDCARD_MATCH_SCORE;
      // no match means no score
      case NONE -> NO_MATCH_SCORE;
    };
  }
}
