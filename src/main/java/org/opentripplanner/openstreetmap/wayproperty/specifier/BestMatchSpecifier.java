package org.opentripplanner.openstreetmap.wayproperty.specifier;

import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

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
  public static final int PREFIX_MATCH_SCORE = 75;
  public static final int NO_MATCH_SCORE = 0;
  private final Condition[] conditions;

  public BestMatchSpecifier(String spec) {
    conditions = OsmSpecifier.parseEqualsTests(spec, ";");
  }

  @Override
  public Scores matchScores(OSMWithTags way) {
    int leftScore = 0, rightScore = 0;
    int leftMatches = 0, rightMatches = 0;

    for (var test : conditions) {
      var leftMatch = test.matchLeft(way);
      var rightMatch = test.matchRight(way);

      int leftTagScore = toTagScore(leftMatch);
      leftScore += leftTagScore;
      if (leftTagScore > 0) {
        leftMatches++;
      }
      int rightTagScore = toTagScore(rightMatch);
      rightScore += rightTagScore;
      if (rightTagScore > 0) {
        rightMatches++;
      }
    }

    int allMatchLeftBonus = (leftMatches == conditions.length) ? 10 : 0;
    leftScore += allMatchLeftBonus;
    int allMatchRightBonus = (rightMatches == conditions.length) ? 10 : 0;
    rightScore += allMatchRightBonus;
    return new Scores(leftScore, rightScore);
  }

  @Override
  public int matchScore(OSMWithTags way) {
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
      // if the test says surface=cobblestone:flattened but the way has surface=cobblestone
      case PREFIX -> PREFIX_MATCH_SCORE;
      // no match means no score
      case NONE -> NO_MATCH_SCORE;
    };
  }
}
