package org.opentripplanner.routing.api.request;

import java.util.EnumSet;
import java.util.Set;
import org.opentripplanner.framework.doc.DocumentedEnum;

public enum StreetMode implements DocumentedEnum<StreetMode> {
  /**
   * No street mode is set. This option is used if we do not want street routing at all in this part
   * of the search.
   */
  NOT_SET(Feature.ACCESS, Feature.TRANSFER, Feature.EGRESS),
  /**
   * Walk only
   */
  WALK(Feature.ACCESS, Feature.TRANSFER, Feature.EGRESS, Feature.WALKING),
  /**
   * Bike only
   */
  BIKE(Feature.ACCESS, Feature.TRANSFER, Feature.EGRESS, Feature.CYCLING),
  /**
   * Bike to a bike parking area, then walk the rest of the way.
   * <p>
   * Direct mode and access mode only.
   */
  BIKE_TO_PARK(Feature.ACCESS, Feature.WALKING, Feature.CYCLING, Feature.PARKING),
  /**
   * Walk to a bike rental point, bike to a bike rental drop-off point, and walk the rest of the
   * way. This can include bike rental at fixed locations or free-floating services.
   */
  BIKE_RENTAL(Feature.ACCESS, Feature.EGRESS, Feature.WALKING, Feature.CYCLING, Feature.RENTING),
  /**
   * Walk to a scooter rental point, ride a scooter to a scooter rental drop-off point, and walk the
   * rest of the way. This can include scooter rental at fixed locations or free-floating services.
   */
  SCOOTER_RENTAL(Feature.ACCESS, Feature.EGRESS, Feature.WALKING, Feature.SCOOTER, Feature.RENTING),
  /**
   * Car only
   */
  CAR(Feature.ACCESS, Feature.TRANSFER, Feature.EGRESS, Feature.DRIVING),
  /**
   * Start in the car, drive to a parking area, and walk the rest of the way.
   * <p>
   * Direct mode and access mode only.
   */
  CAR_TO_PARK(Feature.ACCESS, Feature.WALKING, Feature.DRIVING, Feature.PARKING),
  /**
   * Walk to a pickup point along the road, drive to a drop-off point along the road, and walk the
   * rest of the way. This can include various taxi-services or kiss & ride.
   */
  CAR_PICKUP(Feature.ACCESS, Feature.EGRESS, Feature.WALKING, Feature.DRIVING, Feature.PICKUP),
  /**
   * Walk to a car rental point, drive to a car rental drop-off point and walk the rest of the way.
   * This can include car rental at fixed locations or free-floating services.
   */
  CAR_RENTAL(Feature.ACCESS, Feature.EGRESS, Feature.WALKING, Feature.DRIVING, Feature.RENTING),

  /**
   * Using a car hailing app like Uber or Lyft to get to a train station or all the way to the destination.
   */
  CAR_HAILING(Feature.ACCESS, Feature.EGRESS, Feature.DRIVING, Feature.PICKUP),

  /**
   * Encompasses all types of on-demand and flexible transportation.
   */
  FLEXIBLE(Feature.ACCESS, Feature.EGRESS, Feature.WALKING);

  private enum Feature {
    ACCESS,
    EGRESS,
    TRANSFER,
    WALKING,
    CYCLING,
    DRIVING,
    SCOOTER,
    RENTING,
    PARKING,
    PICKUP,
  }

  private final Set<Feature> features;

  StreetMode(Feature first, Feature... rest) {
    this.features = EnumSet.of(first, rest);
  }

  public boolean accessAllowed() {
    return features.contains(Feature.ACCESS);
  }

  public boolean transferAllowed() {
    return features.contains(Feature.TRANSFER);
  }

  public boolean egressAllowed() {
    return features.contains(Feature.EGRESS);
  }

  public boolean includesWalking() {
    return features.contains(Feature.WALKING);
  }

  public boolean includesBiking() {
    return features.contains(Feature.CYCLING);
  }

  public boolean includesDriving() {
    return features.contains(Feature.DRIVING);
  }

  public boolean includesScooter() {
    return features.contains(Feature.SCOOTER);
  }

  public boolean includesRenting() {
    return features.contains(Feature.RENTING);
  }

  public boolean includesParking() {
    return features.contains(Feature.PARKING);
  }

  public boolean includesPickup() {
    return features.contains(Feature.PICKUP);
  }

  @Override
  public String typeDescription() {
    return "Routing modes on streets, including walking, biking, driving, and car-sharing.";
  }

  private static String GBFS_PREREQ =
    """
    
    _Prerequisite:_ Vehicle or station locations need to be added to OTP from dynamic data feeds.
    See [Configuring GBFS](UpdaterConfig.md#gbfs-vehicle-rental-systems) on how to add one.
    """;

  @Override
  public String enumValueDescription() {
    return switch (this) {
      case NOT_SET -> "";
      case WALK -> "Walking some or all of the way of the route.";
      case BIKE -> """
        Cycling for the entirety of the route or taking a bicycle onto the public transport and cycling from the arrival station to the destination.

        Taking a bicycle onto transit is only possible if information about the permission to do so is supplied in the source data. In GTFS this field
        is called `bikesAllowed`.
        """;
      case BIKE_TO_PARK -> """
        Leaving the bicycle at the departure station and walking from the arrival station to the destination.
        This mode needs to be combined with at least one transit mode otherwise it behaves like an ordinary bicycle journey.
        
        _Prerequisite:_ Bicycle parking stations present in the OSM file and visible to OTP by enabling the property `staticBikeParkAndRide` during graph build.
        """;
      case BIKE_RENTAL -> """
        Taking a rented, shared-mobility bike for part or the entirety of the route.
        """ +
      GBFS_PREREQ;
      case SCOOTER_RENTAL -> """
        Walking to a scooter rental point, riding a scooter to a scooter rental drop-off point, and walking the rest of the way.
        This can include scooter rental at fixed locations or free-floating services.
        """ +
      GBFS_PREREQ;
      case CAR_RENTAL -> """
        Walk to a car rental point, drive to a car rental drop-off point and walk the rest of the way.
        This can include car rental at fixed locations or free-floating services.
        """ +
      GBFS_PREREQ;
      case CAR -> """
        Driving your own car the entirety of the route.
        This can be combined with transit, where will return routes with a [Kiss & Ride](https://en.wikipedia.org/wiki/Park_and_ride#Kiss_and_ride_/_kiss_and_fly) component.
        This means that the car is not parked in a permanent parking area but rather the passenger is dropped off (for example, at an airport) and the driver continues driving the car away from the drop off location.
        """;
      case CAR_TO_PARK -> """
        Driving a car to the park-and-ride facilities near a station and taking publictransport.
        This mode needs to be combined with at least one transit mode otherwise, it behaves like an ordinary car journey.
        _Prerequisite:_ Park-and-ride areas near the stations need to be present in the OSM input file.
        """;
      case CAR_PICKUP -> "Walking to a pickup point along the road, driving to a drop-off point along the road, and walking the rest of the way. <br/> This can include various taxi-services or kiss & ride.";
      case CAR_HAILING -> """
        Using a car hailing app like Uber or Lyft to get to a train station or all the way to the destination.
        
        See [the sandbox documentation](sandbox/RideHailing.md) on how to configure it.
        """;
      case FLEXIBLE -> "Encompasses all types of on-demand and flexible transportation for example GTFS Flex or NeTEx Flexible Stop Places.";
    };
  }
}
