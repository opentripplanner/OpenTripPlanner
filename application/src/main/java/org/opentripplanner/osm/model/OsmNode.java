package org.opentripplanner.osm.model;

import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;

import java.util.OptionalDouble;
import org.locationtech.jts.geom.Coordinate;

public class OsmNode extends OsmEntity {

  public double lat;
  public double lon;

  public OsmNode() {}

  public OsmNode(double lat, double lon) {
    this.lat = lat;
    this.lon = lon;
  }

  public String toString() {
    return "osm node " + id;
  }

  public Coordinate getCoordinate() {
    return new Coordinate(this.lon, this.lat);
  }

  public boolean hasHighwayTrafficLight() {
    return hasTag("highway") && "traffic_signals".equals(getTag("highway"));
  }

  public boolean hasCrossingTrafficLight() {
    return hasTag("crossing") && "traffic_signals".equals(getTag("crossing"));
  }

  /**
   * Checks if this node blocks traversal in any way
   *
   * @return true if it does
   */
  public boolean isBarrier() {
    return overridePermissions(ALL) != ALL;
  }

  /**
   * Checks if this node is a subway station entrance.
   *
   * @return true if it is
   */
  public boolean isSubwayEntrance() {
    return hasTag("railway") && "subway_entrance".equals(getTag("railway"));
  }

  /** checks for units (m/ft) in an OSM ele tag value, and returns the value in meters */
  public OptionalDouble parseEleTag() {
    var ele = getTag("ele");
    if (ele == null) {
      return OptionalDouble.empty();
    }
    ele = ele.toLowerCase();
    double unit = 1;
    if (ele.endsWith("m")) {
      ele = ele.replaceFirst("\\s*m", "");
    } else if (ele.endsWith("ft")) {
      ele = ele.replaceFirst("\\s*ft", "");
      unit = 0.3048;
    }
    try {
      return OptionalDouble.of(Double.parseDouble(ele) * unit);
    } catch (NumberFormatException e) {
      return OptionalDouble.empty();
    }
  }

  @Override
  public String url() {
    return String.format("https://www.openstreetmap.org/node/%d", getId());
  }

  /**
   * Check if this node represents a tagged barrier crossing if placed on an intersection
   * of a highway and a barrier way.
   *
   * @return true if it has a barrier tag, or if it explicitly overrides permissions.
   */
  public boolean isTaggedBarrierCrossing() {
    return (
      hasTag("barrier") ||
      hasTag("access") ||
      hasTag("entrance") ||
      overridePermissions(ALL) != ALL ||
      overridePermissions(NONE) != NONE
    );
  }
}
