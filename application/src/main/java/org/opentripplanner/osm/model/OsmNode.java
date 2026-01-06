package org.opentripplanner.osm.model;

import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;

import java.util.Set;
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

  /**
   * Is this a multi-level node that should be decomposed to multiple coincident nodes? Currently
   * returns true only for elevators.
   *
   * @return whether the node is multi-level
   * @author mattwigway
   */
  public boolean isMultiLevel() {
    return isElevator();
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
   * Checks if this node is a station entrance.
   *
   * @return true if it is
   */
  public boolean isStationEntrance() {
    return (
      isOneOfTags("railway", Set.of("subway_entrance", "train_station_entrance")) ||
      isTag("public_transport", "entrance")
    );
  }

  /**
   * @return True if this entity provides an entrance to a platform or similar entity
   */
  public boolean isEntrance() {
    return (
      (isStationEntrance() || isTag("entrance", "yes") || isTag("entrance", "main")) &&
      !isTag("access", "private") &&
      !isTag("access", "no")
    );
  }

  @Override
  public String url() {
    return String.format("https://www.openstreetmap.org/node/%d", getId());
  }

  /**
   * Check if this node represents a tagged barrier crossing if placed on an intersection of a
   * highway and a barrier way.
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
