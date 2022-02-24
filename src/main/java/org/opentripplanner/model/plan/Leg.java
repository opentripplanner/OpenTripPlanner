package org.opentripplanner.model.plan;

import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.StreetNote;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.core.TraverseMode;

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

    default boolean isWalkingLeg() {
        return false;
    }

    default boolean isOnStreetNonTransit() {
        return false;
    }

    /**
     * The leg's duration in seconds
     */
    default long getDuration() {
        // Round to the closest second; Hence subtract 500 ms before dividing by 1000
        return (500 + getEndTime().getTimeInMillis() - getStartTime().getTimeInMillis()) / 1000;
    }

    /**
     * Return {@code true} if to legs ride the same trip(same tripId) and at least part of the rides
     * overlap. Two legs overlap is they have at least one segment(from one stop to the next) in
     * common.
     */
    default boolean isPartiallySameTransitLeg(Leg other) {
        // Assert both legs are transit legs
        if (!isTransitLeg() || !other.isTransitLeg()) {throw new IllegalStateException();}

        // Must be on the same service date
        if (!getServiceDate().equals(other.getServiceDate())) {return false;}

        // If NOT the same trip, return false
        if (!getTrip().getId().equals(other.getTrip().getId())) {return false;}

        // Return true if legs overlap
        return getBoardStopPosInPattern() < other.getAlightStopPosInPattern()
                && getAlightStopPosInPattern() > other.getBoardStopPosInPattern();
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
    default Trip getTrip() {
        return null;
    }

    /**
     * The mode (e.g., <code>Walk</code>) used when traversing this leg.
     */
    TraverseMode getMode();

    /**
     * The date and time this leg begins.
     */
    Calendar getStartTime();

    /**
     * The date and time this leg ends.
     */
    Calendar getEndTime();

    /**
     * For transit leg, the offset from the scheduled departure-time of the boarding stop in this
     * leg. "scheduled time of departure at boarding stop" = startTime - departureDelay Unit:
     * seconds.
     */
    default int getDepartureDelay() {
        return 0;
    }

    /**
     * For transit leg, the offset from the scheduled arrival-time of the alighting stop in this
     * leg. "scheduled time of arrival at alighting stop" = endTime - arrivalDelay Unit: seconds.
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
     * The best estimate of the time between two arriving vehicles. This is particularly important
     * for non-strict frequency trips, but could become important for real-time trips, strict
     * frequency trips, and scheduled trips with empirical headways.
     */
    default Integer getHeadway() {
        return null;
    }

    /**
     * The distance traveled while traversing the leg in meters.
     */
    Double getDistanceMeters();

    /**
     * The GTFS pathway id
     */
    default FeedScopedId getPathwayId() {
        return null;
    }

    default int getAgencyTimeZoneOffset() {
        TimeZone timeZone = getStartTime().getTimeZone();
        return timeZone.getOffset(getStartTime().getTimeInMillis());
    }

    /**
     * For transit legs, the type of the route. Non transit -1 When 0-7: 0 Tram, 1 Subway, 2 Train,
     * 3 Bus, 4 Ferry, 5 Cable Car, 6 Gondola, 7 Funicular When equal or highter than 100, it is
     * coded using the Hierarchical Vehicle Type (HVT) codes from the European TPEG standard Also
     * see http://groups.google.com/group/gtfs-changes/msg/ed917a69cf8c5bef
     */
    default Integer getRouteType() {
        return null;
    }

    /**
     * For transit legs, the headsign of the bus or train being used. For non-transit legs, null.
     */
    default String getHeadsign() {
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
    default ServiceDate getServiceDate() {
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
     * A series of turn by turn instructions used for walking, biking and driving.
     */
    default List<WalkStep> getWalkSteps() {
        return List.of();
    }

    default Set<StreetNote> getStreetNotes() {
        return null;
    }

    default Set<TransitAlert> getTransitAlerts() {
        return null;
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

    default Boolean getRentedVehicle() {
        return null;
    }

    default String getVehicleRentalNetwork() {
        return null;
    }

    /**
     * If a generalized cost is used in the routing algorithm, this should be the "delta" cost
     * computed by the algorithm for the section this leg account for. This is relevant for anyone
     * who want to debug a search and tuning the system. The unit should be equivalent to the cost
     * of "one second of transit".
     * <p>
     * -1 indicate that the cost is not set/computed.
     */
    int getGeneralizedCost();

    default void addAlert(TransitAlert alert) {
        throw new UnsupportedOperationException();
    }
}