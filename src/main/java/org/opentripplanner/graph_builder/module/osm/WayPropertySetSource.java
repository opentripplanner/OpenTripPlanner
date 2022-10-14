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
    return Source.DEFAULT.getInstance();
  }

  void populateProperties(WayPropertySet wayPropertySet);

  default boolean doesTagValueDisallowThroughTraffic(String tagValue) {
    return (
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
    return isGeneralNoThroughTraffic(way) || doesTagValueDisallowThroughTraffic(vehicle);
  }

  /**
   * Returns true if through traffic for motor vehicles is not allowed.
   */
  default boolean isMotorVehicleThroughTrafficExplicitlyDisallowed(OSMWithTags way) {
    String motorVehicle = way.getTag("motor_vehicle");
    return (
      isVehicleThroughTrafficExplicitlyDisallowed(way) ||
      doesTagValueDisallowThroughTraffic(motorVehicle)
    );
  }

  /**
   * Returns true if through traffic for bicycle is not allowed.
   */
  default boolean isBicycleNoThroughTrafficExplicitlyDisallowed(OSMWithTags way) {
    String bicycle = way.getTag("bicycle");
    return (
      isVehicleThroughTrafficExplicitlyDisallowed(way) ||
      doesTagValueDisallowThroughTraffic(bicycle)
    );
  }

  /**
   * Returns true if through traffic for walk is not allowed.
   */
  default boolean isWalkNoThroughTrafficExplicitlyDisallowed(OSMWithTags way) {
    String foot = way.getTag("foot");
    return isGeneralNoThroughTraffic(way) || doesTagValueDisallowThroughTraffic(foot);
  }

  /**
   * This is the list of WayPropertySetSource sources. The enum provide a mapping between the
   * enum name and the actual implementation.
   */
  enum Source {
    DEFAULT,
    NORWAY,
    UK,
    FINLAND,
    GERMANY,
    ATLANTA,
    HOUSTON;

    public WayPropertySetSource getInstance() {
      return switch (this) {
        case DEFAULT -> new DefaultWayPropertySetSource();
        case NORWAY -> new NorwayWayPropertySetSource();
        case UK -> new UKWayPropertySetSource();
        case FINLAND -> new FinlandWayPropertySetSource();
        case GERMANY -> new GermanyWayPropertySetSource();
        case ATLANTA -> new AtlantaWayPropertySetSource();
        case HOUSTON -> new HoustonWayPropertySetSource();
      };
    }
  }
}
