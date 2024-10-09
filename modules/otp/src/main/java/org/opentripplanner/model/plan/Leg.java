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
import org.opentripplanner.framework.lang.Sandbox;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.plan.legreference.LegReference;
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
  default Duration getDuration() {
    return Duration.between(getStartTime(), getEndTime());
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
    if (!getServiceDate().equals(other.getServiceDate())) {
      return false;
    }

    // If NOT the same trip, return false
    if (!getTrip().getId().equals(other.getTrip().getId())) {
      return false;
    }

    // Return true if legs overlap
    return (
      getBoardStopPosInPattern() < other.getAlightStopPosInPattern() &&
      getAlightStopPosInPattern() > other.getBoardStopPosInPattern()
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
      if (!getTrip().getId().equals(other.getTrip().getId())) {
        return false;
      }

      // Return true if legs overlap in space(have one common stop visit), this is necessary
      // since the same trip id on two following service dates may overlap in time. For example,
      // a trip may run in a loop for 48 hours, overlapping with the same trip id of the trip
      // scheduled for the next service day. They both visit the same stops, with overlapping
      // times, but the stop positions will be different.
      return (
        getBoardStopPosInPattern() < other.getAlightStopPosInPattern() &&
        getAlightStopPosInPattern() > other.getBoardStopPosInPattern()
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
      getStartTime().toEpochSecond() < other.getEndTime().toEpochSecond() &&
      other.getStartTime().toEpochSecond() < getEndTime().toEpochSecond()
    );
  }

  /**
   * For transit legs, the route agency. For non-transit legs {@code null}.
   */
  default Agency getAgency() {
    return null;
  }

  /**
   * For transit legs, the trip operator, fallback to route operator. For non-transit legs {@code
   * null}.
   *
   * @see Trip#getOperator()
   */
  default Operator getOperator() {
    return null;
  }

  /**
   * For transit legs, the route. For non-transit legs, null.
   */
  default Route getRoute() {
    return null;
  }

  /**
   * For transit legs, the trip. For non-transit legs, null.
   */
  @Nullable
  default Trip getTrip() {
    return null;
  }

  /**
   * For transit legs, the trip on service date, if it exists. For non-transit legs, null.
   */
  @Nullable
  default TripOnServiceDate getTripOnServiceDate() {
    return null;
  }

  default Accessibility getTripWheelchairAccessibility() {
    return null;
  }

  /**
   * The time (including realtime information) when the leg starts.
   */
  LegTime start();

  /**
   * The time (including realtime information) when the leg ends.
   */
  LegTime end();

  /**
   * The date and time this leg begins.
   */
  ZonedDateTime getStartTime();

  /**
   * The date and time this leg ends.
   */
  ZonedDateTime getEndTime();

  /**
   * For transit leg, the offset from the scheduled departure-time of the boarding stop in this leg.
   * "scheduled time of departure at boarding stop" = startTime - departureDelay Unit: seconds.
   */
  default int getDepartureDelay() {
    return 0;
  }

  /**
   * For transit leg, the offset from the scheduled arrival-time of the alighting stop in this leg.
   * "scheduled time of arrival at alighting stop" = endTime - arrivalDelay Unit: seconds.
   */
  default int getArrivalDelay() {
    return 0;
  }

  /**
   * Whether there is real-time data about this Leg
   */
  default boolean getRealTime() {
    return false;
  }

  default RealTimeState getRealTimeState() {
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
  default Boolean getNonExactFrequency() {
    return null;
  }

  /**
   * The best estimate of the time between two arriving vehicles. This is particularly important for
   * non-strict frequency trips, but could become important for real-time trips, strict frequency
   * trips, and scheduled trips with empirical headways.
   */
  default Integer getHeadway() {
    return null;
  }

  /**
   * The distance traveled while traversing the leg in meters.
   */
  double getDistanceMeters();

  /**
   * Get the timezone offset in milliseconds.
   */
  default int getAgencyTimeZoneOffset() {
    int MILLIS_TO_SECONDS = 1000;
    return getStartTime().getOffset().getTotalSeconds() * MILLIS_TO_SECONDS;
  }

  /**
   * For transit legs, the type of the route. Non transit -1 When 0-7: 0 Tram, 1 Subway, 2 Train, 3
   * Bus, 4 Ferry, 5 Cable Car, 6 Gondola, 7 Funicular When equal or highter than 100, it is coded
   * using the Hierarchical Vehicle Type (HVT) codes from the European TPEG standard Also see
   * http://groups.google.com/group/gtfs-changes/msg/ed917a69cf8c5bef
   */
  default Integer getRouteType() {
    return null;
  }

  /**
   * For transit legs, the headsign of the bus or train being used. For non-transit legs, null.
   */
  default I18NString getHeadsign() {
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
  default LocalDate getServiceDate() {
    return null;
  }

  /**
   * For transit leg, the route's branding URL (if one exists). For non-transit legs, null.
   */
  default String getRouteBrandingUrl() {
    return null;
  }

  /**
   * The Place where the leg originates.
   */
  Place getFrom();

  /**
   * The Place where the leg begins.
   */
  Place getTo();

  /**
   * For transit legs, intermediate stops between the Place where the leg originates and the Place
   * where the leg ends. For non-transit legs, {@code null}.
   */
  default List<StopArrival> getIntermediateStops() {
    return null;
  }

  /**
   * The leg's geometry.
   */
  LineString getLegGeometry();

  /**
   * The leg's elevation profile.
   *
   * The elevation profile as a comma-separated list of x,y values. x is the distance from the start
   * of the leg, y is the elevation at this distance.
   */
  default ElevationProfile getElevationProfile() {
    return null;
  }

  /**
   * A series of turn by turn instructions used for walking, biking and driving.
   */
  default List<WalkStep> getWalkSteps() {
    return List.of();
  }

  default Set<StreetNote> getStreetNotes() {
    return null;
  }

  default Set<TransitAlert> getTransitAlerts() {
    return Set.of();
  }

  default PickDrop getBoardRule() {
    return null;
  }

  default PickDrop getAlightRule() {
    return null;
  }

  default BookingInfo getDropOffBookingInfo() {
    return null;
  }

  default BookingInfo getPickupBookingInfo() {
    return null;
  }

  default ConstrainedTransfer getTransferFromPrevLeg() {
    return null;
  }

  default ConstrainedTransfer getTransferToNextLeg() {
    return null;
  }

  default Integer getBoardStopPosInPattern() {
    return null;
  }

  default Integer getAlightStopPosInPattern() {
    return null;
  }

  default Integer getBoardingGtfsStopSequence() {
    return null;
  }

  default Integer getAlightGtfsStopSequence() {
    return null;
  }

  /**
   * Is this leg walking with a bike?
   */
  default Boolean getWalkingBike() {
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
  default Float accessibilityScore() {
    return null;
  }

  default Boolean getRentedVehicle() {
    return null;
  }

  default String getVehicleRentalNetwork() {
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
  int getGeneralizedCost();

  default LegReference getLegReference() {
    return null;
  }

  default void addAlert(TransitAlert alert) {
    throw new UnsupportedOperationException();
  }

  default Leg withTimeShift(Duration duration) {
    throw new UnsupportedOperationException();
  }

  default Set<FareZone> getFareZones() {
    var intermediate = getIntermediateStops()
      .stream()
      .flatMap(stopArrival -> stopArrival.place.stop.getFareZones().stream());

    var start = getFareZones(this.getFrom());
    var end = getFareZones(this.getTo());

    return Stream.of(intermediate, start, end).flatMap(s -> s).collect(Collectors.toSet());
  }

  /**
   * Set {@link FareProductUse} for this leg. Their use-id can identify them across several
   * legs.
   */
  @Sandbox
  void setFareProducts(List<FareProductUse> products);

  /**
   * Get the {@link FareProductUse} for this leg.
   */
  @Sandbox
  List<FareProductUse> fareProducts();

  private static Stream<FareZone> getFareZones(Place place) {
    if (place.stop == null) {
      return Stream.empty();
    } else {
      return place.stop.getFareZones().stream();
    }
  }
}
