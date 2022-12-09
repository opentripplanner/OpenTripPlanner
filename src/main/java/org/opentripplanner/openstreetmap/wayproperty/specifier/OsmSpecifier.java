package org.opentripplanner.openstreetmap.wayproperty.specifier;

import java.util.Arrays;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

/**
 * An interface for assigning match scores for OSM entities (mostly ways). The higher the score the
 * better the match.
 * <p>
 * How the scoring logic is implemented is the responsibility of the implementations.
 */
public interface OsmSpecifier {
  static Condition.Equals[] parseEqualsTests(String spec, String separator) {
    return Arrays
      .stream(spec.split(separator))
      .filter(p -> !p.isEmpty())
      .map(pair -> {
        var kv = pair.split("=");
        return new Condition.Equals(kv[0].toLowerCase(), kv[1].toLowerCase());
      })
      .toArray(Condition.Equals[]::new);
  }

  /**
   * Calculates a pair of scores expressing how well an OSM entity's tags match this specifier.
   * <p>
   * Tags in this specifier are matched against those for the left and right side of the OSM way
   * separately. See: http://wiki.openstreetmap.org/wiki/Forward_%26_backward,_left_%26_right
   *
   * @param way an OSM tagged object to compare to this specifier
   */
  Scores matchScores(OSMWithTags way);

  /**
   * Calculates a score expressing how well an OSM entity's tags match this specifier. This does
   * exactly the same thing as {@link OsmSpecifier#matchScores(OSMWithTags)} but without regard for
   * :left and :right.
   */
  int matchScore(OSMWithTags way);

  record Scores(int left, int right) {
    public static Scores of(int s) {
      return new Scores(s, s);
    }
  }
}
