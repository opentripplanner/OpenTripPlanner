package org.opentripplanner.osm.wayproperty.specifier;

import java.util.Arrays;
import org.opentripplanner.osm.model.OsmEntity;

/**
 * An interface for assigning match scores for OSM entities (mostly ways). The higher the score the
 * better the match.
 * <p>
 * How the scoring logic is implemented is the responsibility of the implementations.
 */
public interface OsmSpecifier {
  static Condition[] parseConditions(String spec, String separator) {
    return Arrays.stream(spec.split(separator))
      .filter(p -> !p.isEmpty())
      .map(pair -> {
        var kv = pair.split("=");
        if (kv[1].equals("*")) {
          return new Condition.Present(kv[0].toLowerCase());
        } else {
          return new Condition.Equals(kv[0].toLowerCase(), kv[1].toLowerCase());
        }
      })
      .toArray(Condition[]::new);
  }

  /**
   * Calculates a pair of scores expressing how well an OSM entity's tags match this specifier.
   * <p>
   * Tags in this specifier are matched against those for the forward and backward direction of the OSM way
   * separately. See: http://wiki.openstreetmap.org/wiki/Forward_%26_backward,_left_%26_right
   *
   * @param way an OSM tagged object to compare to this specifier
   */
  Scores matchScores(OsmEntity way);

  /**
   * Calculates a score expressing how well an OSM entity's tags match this specifier. This does
   * exactly the same thing as {@link OsmSpecifier#matchScores(OsmEntity)} but without regard for
   * :left, :right, :forward, :backward and :both.
   */
  int matchScore(OsmEntity way);

  /**
   * Convert this specifier to a human-readable identifier that represents this in (generated)
   * documentation.
   */
  String toDocString();

  record Scores(int forward, int backward) {
    public static Scores of(int s) {
      return new Scores(s, s);
    }
  }
}
