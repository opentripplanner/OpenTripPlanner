package org.opentripplanner.graph_builder.module.osm.specifier;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

/**
 * Specifies a class of OSM tagged entities (e.g. ways) by a list of tags and their values (which
 * may be wildcards). The OSMSpecifier which matches the most tags on an OSM entity will win. In the
 * event that several OSMSpecifiers match the same number of tags, the one that does so using less
 * wildcards will win. For example, if one OSMSpecifier has the tags (highway=residential,
 * cycleway=*) and another has (highway=residential, surface=paved) and a way has the tags
 * (highway=residential, cycleway=lane, surface=paved) the second OSMSpecifier will be applied to
 * that way (2 exact matches beats 1 exact match and a wildcard match).
 * <p>
 * You can also use a logical OR condition to specify a match. This indented to be used with a
 * safety mixin.
 * <p>
 * For example if you specify "lcn=yes|rnc=yes|ncn=yes" then this will match if one of these tags
 * matches.
 * <p>
 * If you would add 3 separate matches that would mean that a way that is tagged with all of them
 * would receive too high a safety value leading to undesired detours.
 * <p>
 * Logical ORs are only implemented for mixins without wildcards.
 */
public class BestMatchSpecifier implements OsmSpecifier {

  private List<Tag> logicalANDPairs = new ArrayList<>(3);
  private List<Tag> logicalORPairs = new ArrayList<>(3);

  public BestMatchSpecifier(String spec) {
    if (spec.contains("|") && spec.contains(";")) {
      throw new RuntimeException(
        String.format(
          "You cannot mix logical AND (';') and logical OR ('|') in same OSM spec: '%s'",
          spec
        )
      );
    } else if (spec.contains("|") && spec.contains("*")) {
      throw new RuntimeException(
        String.format(
          "You cannot mix logical OR ('|') and wildcards ('*') in the same OSM spec: '%s'",
          spec
        )
      );
    } else if (spec.contains("|")) {
      logicalORPairs = OsmSpecifier.getTagsFromString(spec, "\\|");
    } else {
      logicalANDPairs = OsmSpecifier.getTagsFromString(spec, ";");
    }
  }

  @Override
  public Scores matchScores(OSMWithTags match) {
    if (!logicalANDPairs.isEmpty()) {
      return computeANDScore(match);
    } else {
      return computeORScore(match);
    }
  }

  @Override
  public int matchScore(OSMWithTags match) {
    int score = 0;
    int matches = 0;
    for (var pair : logicalANDPairs) {
      String tag = pair.key();
      String value = pair.value();
      String matchValue = match.getTag(tag);
      int tagScore = getTagScore(value, matchValue);
      score += tagScore;
      if (tagScore > 0) {
        matches += 1;
      }
    }
    score += matches == logicalANDPairs.size() ? 10 : 0;
    return score;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (var pair : logicalANDPairs) {
      builder.append(pair.key());
      builder.append("=");
      builder.append(pair.value());
      builder.append(";");
    }
    for (var pair : logicalORPairs) {
      builder.append(pair.value());
      builder.append("=");
      builder.append(pair.value());
      builder.append("|");
    }
    return builder.toString();
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
      return 100;
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

  private Scores computeORScore(OSMWithTags match) {
    // not sure if we should calculate a proper score as it doesn't make a huge amount of sense to do it for
    // logical OR conditions
    var oneOfORPairMatches = logicalORPairs
      .stream()
      .anyMatch(pair -> match.isTag(pair.key(), pair.value()));
    if (oneOfORPairMatches) {
      return new Scores(1, 1);
    } else return new Scores(0, 0);
  }

  private Scores computeANDScore(OSMWithTags way) {
    int leftScore = 0, rightScore = 0;
    int leftMatches = 0, rightMatches = 0;

    for (var pair : logicalANDPairs) {
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

    int allMatchLeftBonus = (leftMatches == logicalANDPairs.size()) ? 10 : 0;
    leftScore += allMatchLeftBonus;
    int allMatchRightBonus = (rightMatches == logicalANDPairs.size()) ? 10 : 0;
    rightScore += allMatchRightBonus;
    return new Scores(leftScore, rightScore);
  }
}
