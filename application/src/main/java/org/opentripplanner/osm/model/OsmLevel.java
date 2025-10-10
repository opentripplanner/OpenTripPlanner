package org.opentripplanner.osm.model;

import java.util.Objects;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class OsmLevel implements Comparable<OsmLevel> {

  private final double level;
  private final String name;

  public OsmLevel(double level, String name) {
    this.name = Objects.requireNonNull(name);
    this.level = level;
  }

  @Override
  public int compareTo(OsmLevel other) {
    return Double.compare(this.level, other.level);
  }

  @Override
  public int hashCode() {
    return Double.hashCode(level);
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof OsmLevel other) {
      // Levels should be equal if their 0-based number representations are equal.
      // There should be no need to compare names.
      // For example, comparing the default level and a ground level with a name should be equal.
      return this.level == other.level;
    } else {
      return false;
    }
  }

  /**
   * 0-based level that can be negative.
   * See https://wiki.openstreetmap.org/wiki/Key:level
   * and https://wiki.openstreetmap.org/wiki/Key:layer.
   */
  public double level() {
    return this.level;
  }

  /**
   * See https://wiki.openstreetmap.org/wiki/Key:level:ref.
   */
  public String name() {
    return this.name;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(getClass()).addNum("level", level).addStr("name", name).toString();
  }
}
