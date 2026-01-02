package org.opentripplanner.osm.model;

import java.util.Objects;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class OsmLevel implements Comparable<OsmLevel> {

  private final double level;
  private final String name;
  private final OsmLevelSource source;

  public OsmLevel(double level, String name, OsmLevelSource source) {
    this.name = Objects.requireNonNull(name);
    this.level = level;
    this.source = source;
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OsmLevel that = (OsmLevel) o;
    // Levels should be equal if their 0-based number representations are equal.
    // There should be no need to compare names.
    // For example, comparing the default level and a ground level with a name should be equal.
    return this.level == that.level;
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

  /**
   * What is the source of the information for this level.
   */
  public OsmLevelSource source() {
    return this.source;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(getClass()).addNum("level", level).addStr("name", name).toString();
  }
}
