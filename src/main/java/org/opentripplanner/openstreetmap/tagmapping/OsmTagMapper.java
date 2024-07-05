package org.opentripplanner.openstreetmap.tagmapping;

import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.openstreetmap.wayproperty.WayPropertySet;

/**
 * Interface for populating a {@link WayPropertySet} that determine how OSM streets can be traversed
 * in various modes and named.
 *
 * @author bdferris, novalis, seime
 */
public interface OsmTagMapper {
  void populateProperties(WayPropertySet wayPropertySet);

  default boolean doesTagValueDisallowThroughTraffic(String tagValue) {
    return (
      "no".equals(tagValue) ||
      "destination".equals(tagValue) ||
      "private".equals(tagValue) ||
      "customers".equals(tagValue) ||
      "delivery".equals(tagValue)
    );
  }

  default float getCarSpeedForWay(OSMWithTags way, boolean backward) {
    return way.getOsmProvider().getWayPropertySet().getCarSpeedForWay(way, backward);
  }

  default Float getMaxUsedCarSpeed(WayPropertySet wayPropertySet) {
    return wayPropertySet.maxUsedCarSpeed;
  }

  default boolean isGeneralNoThroughTraffic(OSMWithTags way) {
    String access = way.getTag("access");
    return doesTagValueDisallowThroughTraffic(access);
  }

  default boolean isVehicleThroughTrafficExplicitlyDisallowed(OSMWithTags way) {
    String vehicle = way.getTag("vehicle");
    if (vehicle != null) {
      return doesTagValueDisallowThroughTraffic(vehicle);
    } else {
      return isGeneralNoThroughTraffic(way);
    }
  }

  /**
   * Returns true if through traffic for motor vehicles is not allowed.
   */
  default boolean isMotorVehicleThroughTrafficExplicitlyDisallowed(OSMWithTags way) {
    String motorVehicle = way.getTag("motor_vehicle");
    if (motorVehicle != null) {
      return doesTagValueDisallowThroughTraffic(motorVehicle);
    } else {
      return isVehicleThroughTrafficExplicitlyDisallowed(way);
    }
  }

  /**
   * Returns true if through traffic for bicycle is not allowed.
   */
  default boolean isBicycleNoThroughTrafficExplicitlyDisallowed(OSMWithTags way) {
    String bicycle = way.getTag("bicycle");
    if (bicycle != null) {
      return doesTagValueDisallowThroughTraffic(bicycle);
    } else {
      return isVehicleThroughTrafficExplicitlyDisallowed(way);
    }
  }

  /**
   * Returns true if through traffic for walk is not allowed.
   */
  default boolean isWalkNoThroughTrafficExplicitlyDisallowed(OSMWithTags way) {
    String foot = way.getTag("foot");
    if (foot != null) {
      return doesTagValueDisallowThroughTraffic(foot);
    } else {
      return isGeneralNoThroughTraffic(way);
    }
  }
}
