package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.openstreetmap.model.OSMWithTags;

/**
 * Interface for populating a {@link WayPropertySet} that determine how OSM streets can be traversed
 * in various modes and named.
 *
 * @author bdferris, novalis, seime
 */
public interface WayPropertySetSource {
  static WayPropertySetSource defaultWayPropertySetSource() {
    return fromConfig("default");
  }

  /**
   * Return the given WayPropertySetSource or throws IllegalArgumentException if an unknown type is
   * specified
   */
  static WayPropertySetSource fromConfig(String type) {
    // type is set to "default" by GraphBuilderParameters if not provided in
    // build-config.json
    return switch (type) {
      case "default" -> new DefaultWayPropertySetSource();
      case "norway" -> new NorwayWayPropertySetSource();
      case "uk" -> new UKWayPropertySetSource();
      case "finland" -> new FinlandWayPropertySetSource();
      case "germany" -> new GermanyWayPropertySetSource();
      case "atlanta" -> new AtlantaWayPropertySetSource();
      case "houston" -> new HoustonWayPropertySetSource();
      default -> throw new IllegalArgumentException(
        "Unknown osmWayPropertySet: '%s'".formatted(type)
      );
    };
  }

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
