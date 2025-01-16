package org.opentripplanner.osm.model;

import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.street.model.StreetTraversalPermission;

public class OsmNode extends OsmWithTags {

  static final Set<String> MOTOR_VEHICLE_BARRIERS = Set.of("bollard", "bar", "chain");

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
   * Checks if this node is a barrier which prevents motor vehicle traffic.
   *
   * @return true if it is
   */
  public boolean isMotorVehicleBarrier() {
    return isOneOfTags("barrier", MOTOR_VEHICLE_BARRIERS);
  }

  /**
   * Checks if this node blocks traversal in any way
   *
   * @return true if it does
   */
  public boolean isBarrier() {
    return (
      isMotorVehicleBarrier() ||
      isPedestrianExplicitlyDenied() ||
      isBicycleExplicitlyDenied() ||
      isMotorcarExplicitlyDenied() ||
      isMotorVehicleExplicitlyDenied() ||
      isGeneralAccessDenied()
    );
  }

  /**
   * Checks if this node is a subway station entrance.
   *
   * @return true if it is
   */
  public boolean isSubwayEntrance() {
    return hasTag("railway") && "subway_entrance".equals(getTag("railway"));
  }

  /**
   * Consider barrier tag in  permissions. Leave the rest for the super class.
   */
  @Override
  public StreetTraversalPermission overridePermissions(StreetTraversalPermission def) {
    StreetTraversalPermission permission = def;
    if (isMotorVehicleBarrier()) {
      permission = permission.remove(StreetTraversalPermission.CAR);
    }
    return super.overridePermissions(permission);
  }

  @Override
  public String url() {
    return String.format("https://www.openstreetmap.org/node/%d", getId());
  }
}
