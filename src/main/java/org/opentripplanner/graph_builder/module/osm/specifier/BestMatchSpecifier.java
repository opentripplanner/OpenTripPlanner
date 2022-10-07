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
  private final List<Tag> pairs;

  public BestMatchSpecifier(String spec) {
    pairs = OsmSpecifier.getTagsFromString(spec, ";");
  }

  @Override
  public Scores matchScores(OSMWithTags way) {
    return computeScores(way);
  }

  @Override
  public int matchScore(OSMWithTags way) {
    int score = 0;
    int matches = 0;
    for (var pair : pairs) {
      String tag = pair.key();
      String value = pair.value();
      String matchValue = way.getTag(tag);
      int tagScore = getTagScore(value, matchValue);
      score += tagScore;
      if (tagScore > 0) {
        matches += 1;
      }
    }
    score += matches == pairs.size() ? 10 : 0;
    return score;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass()).addObj("pairs", pairs).toString();
  }

  /**
   * Calculates a score indicating how well an OSM tag value matches the given matchValue. An exact
   * match is worth 100 points, a partial match on the part of the value before a colon is worth 75
   * points, and a wildcard match is worth only one point, to serve as a tiebreaker. A score of 0
   * means they do not match.
   */
  private static int getTagScore(String value, String matchValue) {
    // either this matches on a wildcard, or it matches exactly
    if (OsmSpecifier.matchesWildcard(value, matchValue)) {
      return 1; // wildcard matches are basically tiebreakers
    } else if (value.equals(matchValue)) {
      return EXACT_MATCH_SCORE;
    } else {
      if (value.contains(":")) {
        // treat cases like cobblestone:flattened as cobblestone if a more-specific match
        // does not apply
        value = value.split(":", 2)[0];
        if (value.equals(matchValue)) {
          return 75;
        } else {
          return 0;
        }
      } else {
        return 0;
      }
    }
  }

  private Scores computeScores(OSMWithTags way) {
    int leftScore = 0, rightScore = 0;
    int leftMatches = 0, rightMatches = 0;

    for (var pair : pairs) {
      String tag = pair.key();
      String value = pair.value();
      String leftMatchValue = way.getTag(tag + ":left");
      String rightMatchValue = way.getTag(tag + ":right");
      String matchValue = way.getTag(tag);
      if (leftMatchValue == null) {
        leftMatchValue = matchValue;
      }
      if (rightMatchValue == null) {
        rightMatchValue = matchValue;
      }

      int leftTagScore = getTagScore(value, leftMatchValue);
      leftScore += leftTagScore;
      if (leftTagScore > 0) {
        leftMatches++;
      }
      int rightTagScore = getTagScore(value, rightMatchValue);
      rightScore += rightTagScore;
      if (rightTagScore > 0) {
        rightMatches++;
      }
    }

    int allMatchLeftBonus = (leftMatches == pairs.size()) ? 10 : 0;
    leftScore += allMatchLeftBonus;
    int allMatchRightBonus = (rightMatches == pairs.size()) ? 10 : 0;
    rightScore += allMatchRightBonus;
    return new Scores(leftScore, rightScore);
  }
}
