package org.opentripplanner.model.plan;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.plan.leg.ElevationProfile;
import org.opentripplanner.model.plan.leg.LegCallTime;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.model.plan.leg.StopArrival;
import org.opentripplanner.model.plan.legreference.LegReference;
import org.opentripplanner.model.plan.walkstep.WalkStep;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.street.model.note.StreetNote;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.FareZone;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.opentripplanner.utils.lang.Sandbox;

/**
 * One leg of a trip -- that is, a temporally continuous piece of the journey that takes place on a
 * particular vehicle or on the street using mainly a single mode
 */
public interface Leg {
  /**
   * Whether this leg is a transit leg or not.
   *
   * @return Boolean true if the leg is a transit leg
   */
  boolean isTransitLeg();

  default boolean isScheduledTransitLeg() {
    return false;
  }

  default ScheduledTransitLeg asScheduledTransitLeg() {
    throw new ClassCastException();
  }

  /**
   * For transit legs, if the rider should stay on the vehicle as it changes route names. This is
   * the same as a stay-seated transfer.
   */
  default Boolean isInterlinedWithPreviousLeg() {
    return false;
  }

  /** The mode is walking. */
  default boolean isWalkingLeg() {
    return false;
  }

  /**
   * The mode is a street mode; Hence not a transit mode.
   */
  default boolean isStreetLeg() {
    return false;
  }

  /**
   * The leg's duration in seconds
   */
  default Duration duration() {
    return Duration.between(startTime(), endTime());
  }

  /**
   * Return {@code true} if to legs ride the same trip(same tripId) and at least part of the rides
   * overlap. Two legs overlap is they have at least one segment(from one stop to the next) in
   * common.
   */
  default boolean isPartiallySameTransitLeg(Leg other) {
    // Assert both legs are transit legs
    if (!isTransitLeg() || !other.isTransitLeg()) {
      throw new IllegalStateException();
    }

    // Must be on the same service date
    if (!serviceDate().equals(other.serviceDate())) {
      return false;
    }

    // If NOT the same trip, return false
    if (!trip().getId().equals(other.trip().getId())) {
      return false;
    }

    // Return true if legs overlap
    return (
      boardStopPosInPattern() < other.alightStopPosInPattern() &&
      alightStopPosInPattern() > other.boardStopPosInPattern()
    );
  }

  /**
   * Check is this instance has the same type and mode as the given other.
   */
  boolean hasSameMode(Leg other);

  /**
   * Return {@code true} if to legs are the same. The mode must match and the time must overlap.
   * For transit the trip ID must match and board/alight position must overlap. (Two trips with
   * different service-date can overlap in time, so we use boarding-/alight-position to verify).
   */
  default boolean isPartiallySameLeg(Leg other) {
    if (!hasSameMode(other)) {
      return false;
    }

    // Overlap in time
    if (!overlapInTime(other)) {
      return false;
    }

    // The mode is the same, so this and the other are both *street* or *transit* legs
    if (isStreetLeg()) {
      return true;
    }
    // Transit leg
    else {
      // If NOT the same trip, return false
      if (!trip().getId().equals(other.trip().getId())) {
        return false;
      }

      // Return true if legs overlap in space(have one common stop visit), this is necessary
      // since the same trip id on two following service dates may overlap in time. For example,
      // a trip may run in a loop for 48 hours, overlapping with the same trip id of the trip
      // scheduled for the next service day. They both visit the same stops, with overlapping
      // times, but the stop positions will be different.
      return (
        boardStopPosInPattern() < other.alightStopPosInPattern() &&
        alightStopPosInPattern() > other.boardStopPosInPattern()
      );
    }
  }

  /**
   * Return true if this leg and the given {@code other} leg overlap in time. If the
   * start-time equals the end-time this method returns false.
   */
  default boolean overlapInTime(Leg other) {
    return (
      // We convert to epoch seconds to ignore nanos (save CPU),
      // in favor of using the methods isAfter(...) and isBefore(...)
      startTime().toEpochSecond() < other.endTime().toEpochSecond() &&
      other.startTime().toEpochSecond() < endTime().toEpochSecond()
    );
  }

  /**
   * For transit legs, the route agency. For non-transit legs {@code null}.
   */
  @Nullable
  default Agency agency() {
    return null;
  }

  /**
   * For transit legs, the trip operator, fallback to route operator. For non-transit legs {@code
   * null}.
   *
   * @see Trip#getOperator()
   */
  @Nullable
  default Operator operator() {
    return null;
  }

  /**
   * For transit legs, the route. For non-transit legs, null.
   */
  @Nullable
  default Route route() {
    return null;
  }

  /**
   * For transit legs, the trip. For non-transit legs, null.
   */
  @Nullable
  default Trip trip() {
    return null;
  }

  /**
   * For transit legs, the trip on service date, if it exists. For non-transit legs, null.
   */
  @Nullable
  default TripOnServiceDate tripOnServiceDate() {
    return null;
  }

  @Nullable
  default Accessibility tripWheelchairAccessibility() {
    return null;
  }

  /**
   * The time (including realtime information) when the leg starts.
   */
  LegCallTime start();

  /**
   * The time (including realtime information) when the leg ends.
   */
  LegCallTime end();

  /**
   * The date and time this leg begins.
   * TODO Does the start-time incorporate slack and/or wait-time? - This should be documented!
   */
  ZonedDateTime startTime();

  /**
   * The date and time this leg ends.
   * TODO Does the end-time incorporate slack and/or wait-time? - This should be documented!
   */
  ZonedDateTime endTime();

  /**
   * For transit leg, the offset from the scheduled departure-time of the boarding stop in this leg.
   * "scheduled time of departure at boarding stop" = startTime - departureDelay Unit: seconds.
   */
  default int departureDelay() {
    return 0;
  }

  /**
   * For transit leg, the offset from the scheduled arrival-time of the alighting stop in this leg.
   * "scheduled time of arrival at alighting stop" = endTime - arrivalDelay Unit: seconds.
   */
  default int arrivalDelay() {
    return 0;
  }

  /**
   * Whether there is real-time data about this Leg
   */
  default boolean isRealTimeUpdated() {
    return false;
  }

  @Nullable
  default RealTimeState realTimeState() {
    return null;
  }

  /**
   * Whether this Leg describes a flexible trip. The reason we need this is that FlexTrip does not
   * inherit from Trip, so that the information that the Trip is flexible would be lost when
   * creating this object.
   */
  default boolean isFlexibleTrip() {
    return false;
  }

  /**
   * Is this a frequency-based trip with non-strict departure times?
   */
  @Nullable
  default Boolean isNonExactFrequency() {
    return null;
  }

  /**
   * The best estimate of the time between two arriving vehicles. This is particularly important for
   * non-strict frequency trips, but could become important for real-time trips, strict frequency
   * trips, and scheduled trips with empirical headways.
   */
  @Nullable
  default Integer headway() {
    return null;
  }

  /**
   * The distance traveled while traversing the leg in meters.
   */
  double distanceMeters();

  /**
   * Get the timezone offset in milliseconds.
   */
  default int agencyTimeZoneOffset() {
    int MILLIS_TO_SECONDS = 1000;
    return startTime().getOffset().getTotalSeconds() * MILLIS_TO_SECONDS;
  }

  /**
   * For transit legs, the type of the route. Non transit -1 When 0-7: 0 Tram, 1 Subway, 2 Train, 3
   * Bus, 4 Ferry, 5 Cable Tram, 6 Gondola, 7 Funicular When equal or highter than 100, it is coded
   * using the Hierarchical Vehicle Type (HVT) codes from the European TPEG standard Also see
   * http://groups.google.com/group/gtfs-changes/msg/ed917a69cf8c5bef
   */
  @Nullable
  default Integer routeType() {
    return null;
  }

  /**
   * For transit legs, the headsign of the bus or train being used. For non-transit legs, null.
   */
  @Nullable
  default I18NString headsign() {
    return null;
  }

  /**
   * For transit legs, the service date of the trip. For non-transit legs, null.
   * <p>
   * The trip service date should be used to identify the correct trip schedule and can not be
   * trusted to display the date for any departures or arrivals. For example, the first departure
   * for a given trip may happen at service date March 25th and service time 25:00, which in local
   * time would be Mach 26th 01:00.
   */
  @Nullable
  default LocalDate serviceDate() {
    return null;
  }

  /**
   * For transit leg, the route's branding URL (if one exists). For non-transit legs, null.
   */
  @Nullable
  default String routeBrandingUrl() {
    return null;
  }

  /**
   * The Place where the leg originates.
   */
  Place from();

  /**
   * The Place where the leg begins.
   */
  Place to();

  /**
   * For transit legs, intermediate stops between the Place where the leg originates and the Place
   * where the leg ends. For non-transit legs, {@code null}.
   */
  @Nullable
  default List<StopArrival> listIntermediateStops() {
    return null;
  }

  /**
   * The leg's geometry.
   */
  @Nullable
  LineString legGeometry();

  /**
   * The leg's elevation profile.
   *
   * The elevation profile as a comma-separated list of x,y values. x is the distance from the start
   * of the leg, y is the elevation at this distance.
   */
  @Nullable
  default ElevationProfile elevationProfile() {
    return null;
  }

  /**
   * A series of turn by turn instructions used for walking, biking and driving.
   */
  default List<WalkStep> listWalkSteps() {
    return List.of();
  }

  default Set<StreetNote> listStreetNotes() {
    return Set.of();
  }

  Set<TransitAlert> listTransitAlerts();

  @Nullable
  default PickDrop boardRule() {
    return null;
  }

  @Nullable
  default PickDrop alightRule() {
    return null;
  }

  @Nullable
  default BookingInfo dropOffBookingInfo() {
    return null;
  }

  @Nullable
  default BookingInfo pickupBookingInfo() {
    return null;
  }

  @Nullable
  default ConstrainedTransfer transferFromPrevLeg() {
    return null;
  }

  @Nullable
  default ConstrainedTransfer transferToNextLeg() {
    return null;
  }

  @Nullable
  default Integer boardStopPosInPattern() {
    return null;
  }

  @Nullable
  default Integer alightStopPosInPattern() {
    return null;
  }

  @Nullable
  default Integer boardingGtfsStopSequence() {
    return null;
  }

  @Nullable
  default Integer alightGtfsStopSequence() {
    return null;
  }

  /**
   * Is this leg walking with a bike?
   */
  @Nullable
  default Boolean walkingBike() {
    return null;
  }

  /**
   * A sandbox feature for calculating a numeric score between 0 and 1 which indicates
   * how accessible the itinerary is as a whole. This is not a very scientific method but just
   * a rough guidance that expresses certainty or uncertainty about the accessibility.
   *
   * The intended audience for this score are frontend developers wanting to show a simple UI
   * rather than having to iterate over all the stops and trips.
   *
   * Note: the information to calculate this score are all available to the frontend, however
   * calculating them on the backend makes life a little easier and changes are automatically
   * applied to all frontends.
   */
  @Nullable
  @Sandbox
  default Float accessibilityScore() {
    return null;
  }

  @Nullable
  @Sandbox
  Emission emissionPerPerson();

  @Nullable
  @Sandbox
  Leg withEmissionPerPerson(Emission emissionPerPerson);

  @Nullable
  default Boolean rentedVehicle() {
    return null;
  }

  @Nullable
  default String vehicleRentalNetwork() {
    return null;
  }

  /**
   * If a generalized cost is used in the routing algorithm, this should be the "delta" cost
   * computed by the algorithm for the section this leg account for. This is relevant for anyone who
   * want to debug a search and tuning the system. The unit should be equivalent to the cost of "one
   * second of transit".
   * <p>
   * -1 indicate that the cost is not set/computed.
   */
  int generalizedCost();

  @Nullable
  default LegReference legReference() {
    return null;
  }

  default Leg withTimeShift(Duration duration) {
    throw new UnsupportedOperationException();
  }

  default Set<FareZone> fareZones() {
    var intermediate = listIntermediateStops()
      .stream()
      .flatMap(stopArrival -> stopArrival.place.stop.getFareZones().stream());

    var start = fareZones(this.from());
    var end = fareZones(this.to());

    return Stream.of(intermediate, start, end).flatMap(s -> s).collect(Collectors.toSet());
  }

  /**
   * Get the {@link FareProductUse} for this leg.
   */
  @Sandbox
  List<FareProductUse> fareProducts();

  private static Stream<FareZone> fareZones(Place place) {
    if (place.stop == null) {
      return Stream.empty();
    } else {
      return place.stop.getFareZones().stream();
    }
  }
}
