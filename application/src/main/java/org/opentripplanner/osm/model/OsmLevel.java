package org.opentripplanner.osm.model;

import java.util.Objects;

public class OsmLevel implements Comparable<OsmLevel> {

  private final double level;
  private final String name;

  public OsmLevel(double level, String name) {
    Objects.requireNonNull(name);
    this.level = level;
    this.name = name;
  }

  @Override
  public int compareTo(OsmLevel other) {
    return Double.compare(this.level, other.level);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.level, this.name);
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof OsmLevel other) {
      return this.level == other.level && this.name.equals(other.name);
    } else {
      return false;
    }
  }

  /**
   * 0-based level that can be negative.
   * See https://wiki.openstreetmap.org/wiki/Key:level
   * and https://wiki.openstreetmap.org/wiki/Key:layer.
   */
  public double getLevel() {
    return this.level;
  }

  /**
   * See https://wiki.openstreetmap.org/wiki/Key:level:ref.
   */
  public String getName() {
    return this.name;
  }
}
