package org.opentripplanner.graph_builder.module.osm.specifier;

import java.util.List;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.util.lang.ToStringBuilder;

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
  private final List<Operation> operations;

  public BestMatchSpecifier(String spec) {
    operations = OsmSpecifier.parseOperations(spec, ";");
  }

  @Override
  public Scores matchScores(OSMWithTags way) {
    int leftScore = 0, rightScore = 0;
    int leftMatches = 0, rightMatches = 0;

    for (var op : operations) {
      var mainMatch = op.match(way);
      var leftMatch = op.matchLeft(way).ifNone(mainMatch);
      var rightMatch = op.matchRight(way).ifNone(mainMatch);

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

    int allMatchLeftBonus = (leftMatches == operations.size()) ? 10 : 0;
    leftScore += allMatchLeftBonus;
    int allMatchRightBonus = (rightMatches == operations.size()) ? 10 : 0;
    rightScore += allMatchRightBonus;
    return new Scores(leftScore, rightScore);
  }

  @Override
  public int matchScore(OSMWithTags way) {
    int score = 0;
    int matches = 0;
    for (var op : operations) {
      var matchValue = op.match(way);
      int tagScore = toTagScore(matchValue);
      score += tagScore;
      if (tagScore > 0) {
        matches += 1;
      }
    }
    score += matches == operations.size() ? 10 : 0;
    return score;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass()).addObj("pairs", operations).toString();
  }

  /**
   * Calculates a score indicating how well an OSM tag value matches the given matchValue. An exact
   * match is worth 100 points, a partial match on the part of the value before a colon is worth 75
   * points, and a wildcard match is worth only one point, to serve as a tiebreaker. A score of 0
   * means they do not match.
   */
  private static int toTagScore(Operation.MatchResult res) {
    return switch (res) {
      case EXACT -> EXACT_MATCH_SCORE;
      // wildcard matches are basically tiebreakers
      case WILDCARD -> 1;
      // if the op says surface=cobblestone:flattened but the way has surface=cobblestone
      case PREFIX -> 75;
      // no match means no score
      case PARTIAL, NONE -> 0;
    };
  }
}
