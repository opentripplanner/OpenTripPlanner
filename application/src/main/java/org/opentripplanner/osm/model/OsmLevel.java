package org.opentripplanner.osm.model;

public class OsmLevel implements Comparable<OsmLevel> {

  private final double level;
  private final String name;

  public OsmLevel(double level, String name) {
    this.level = level;
    this.name = name;
  }

  @Override
  public int compareTo(OsmLevel other) {
    return Double.compare(this.level, other.level);
  }

  @Override
  public int hashCode() {
    return Double.hashCode(this.level);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (!(other instanceof OsmLevel)) {
      return false;
    }
    return this.level == ((OsmLevel) other).level;
  }

  /** 0-based level.
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
