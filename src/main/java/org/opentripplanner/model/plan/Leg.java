package org.opentripplanner.model.plan;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.StreetNote;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.util.model.EncodedPolylineBean;

/**
* One leg of a trip -- that is, a temporally continuous piece of the journey that takes place on a
* particular vehicle (or on foot).
*/
public class Leg {

    private final TraverseMode mode;

    private final Trip trip;

    private Calendar startTime = null;

    private Calendar endTime = null;

    private int departureDelay = 0;

    private int arrivalDelay = 0;

    private Boolean realTime = false;

    private boolean flexibleTrip = false;

    private Boolean isNonExactFrequency = null;

    private Integer headway = null;

    private Double distanceMeters = null;

    private Boolean pathway = false;

    private FeedScopedId pathwayId;

    private int agencyTimeZoneOffset;

    private Integer routeType = null;

    private String headsign = null;

    private ServiceDate serviceDate = null;

    private String routeBrandingUrl = null;

    private Place from = null;

    private Place to = null;

    private List<StopArrival> intermediateStops;

    private EncodedPolylineBean legGeometry;

    private List<WalkStep> walkSteps;

    private Set<StreetNote> streetNotes = new HashSet<>();

    private Set<TransitAlert> transitAlerts = new HashSet<>();

    private String boardRule;

    private String alightRule;

    private BookingInfo dropOffBookingInfo = null;

    private BookingInfo pickupBookingInfo = null;

    private ConstrainedTransfer transferFromPrevLeg = null;

    private ConstrainedTransfer transferToNextLeg = null;

    public Integer boardStopPosInPattern = null;

    public Integer alightStopPosInPattern = null;

    private Integer boardingGtfsStopSequence = null;

    private Integer alightGtfsStopSequence = null;

    private Boolean walkingBike;

    private Boolean rentedVehicle;

    private String vehicleRentalNetwork;

    private int generalizedCost = -1;

    public Leg(TraverseMode mode) {
        if (mode.isTransit()) {
            throw new IllegalArgumentException(
                    "To create a transit leg use the other constructor.");
        }
        this.mode = mode;
        this.trip = null;
    }

    public Leg(Trip trip) {
        this.mode = TraverseMode.fromTransitMode(trip.getMode());
        this.trip = trip;
    }

    /**
     * Whether this leg is a transit leg or not.
     *
     * @return Boolean true if the leg is a transit leg
     */
    public boolean isTransitLeg() {
        return mode.isTransit();
    }

    /**
     * For transit legs, if the rider should stay on the vehicle as it changes route names. This is
     * the same as a stay-seated transfer.
     */
    public Boolean isInterlinedWithPreviousLeg() {
        if (transferFromPrevLeg == null) {return false;}
        return transferFromPrevLeg.getTransferConstraint().isStaySeated();
    }

    /**
     * A scheduled leg is a leg riding a public scheduled transport including frequency based
     * transport, or flex service. If the ride is not likely to wait for the passenger, even if the
     * passenger call in and say hen is late, then this method should return {@code true}.
     * <p>
     * For example, this method can be used to add extra "cost" if the transfer time is tight.
     */
    public boolean isScheduled() {
        return isTransitLeg() || flexibleTrip;
    }

    public boolean isWalkingLeg() {
        return mode.isWalking();
    }

    public boolean isOnStreetNonTransit() {
        return mode.isOnStreetNonTransit();
    }

    /**
     * The leg's duration in seconds
     */
    public long getDuration() {
        // Round to closest second; Hence subtract 500 ms before dividing by 1000
        return (500 + endTime.getTimeInMillis() - startTime.getTimeInMillis()) / 1000;
    }

    public void addStretNote(StreetNote streetNote) {
        streetNotes.add(streetNote);
    }

    public void addAlert(TransitAlert alert) {
        transitAlerts.add(alert);
    }

    public void setVehicleRentalNetwork(String network) {
        vehicleRentalNetwork = network;
    }

    /**
     * Return {@code true} if to legs ride the same trip(same tripId) and at least part of the rides
     * overlap. Two legs overlap is they have at least one segment(from one stop to the next) in
     * common.
     */
    public boolean isPartiallySameTransitLeg(Leg other) {
        // Assert both legs are transit legs
        if (!isTransitLeg() || !other.isTransitLeg()) {throw new IllegalStateException();}

        // Must be on the same service date
        if (!serviceDate.equals(other.serviceDate)) {return false;}

        // If NOT the same trip, return false
        if (!trip.getId().equals(other.trip.getId())) {return false;}

        // Return true if legs overlap
        return boardStopPosInPattern < other.alightStopPosInPattern &&
               alightStopPosInPattern > other.boardStopPosInPattern;
    }

    /**
     * For transit legs, the route agency. For non-transit legs {@code null}.
     */
    public Agency getAgency() {
        return isTransitLeg() ? getRoute().getAgency() : null;
    }

    /**
     * For transit legs, the trip operator, fallback to route operator. For non-transit legs {@code
     * null}.
     *
     * @see Trip#getOperator()
     */
    public Operator getOperator() {
        return isTransitLeg() ? trip.getOperator() : null;
    }

    /**
     * For transit legs, the route. For non-transit legs, null.
     */
    public Route getRoute() {return isTransitLeg() ? trip.getRoute() : null;}

    /**
     * For transit legs, the trip. For non-transit legs, null.
     */
    public Trip getTrip() {return trip;}

    /**
     * Should be used for debug logging only
     */
    @Override
    public String toString() {
        return ToStringBuilder.of(Leg.class)
                .addObj("from", from)
                .addObj("to", to)
                .addTimeCal("startTime", startTime)
                .addTimeCal("endTime", endTime)
                .addNum("departureDelay", departureDelay, 0)
                .addNum("arrivalDelay", arrivalDelay, 0)
                .addBool("realTime", realTime)
                .addBool("isNonExactFrequency", isNonExactFrequency)
                .addNum("headway", headway)
                .addEnum("mode", mode)
                .addNum("distance", distanceMeters, "m")
                .addNum("cost", generalizedCost)
                .addBool("pathway", pathway)
                .addObj("gtfsPathwayId", pathwayId)
                .addNum("agencyTimeZoneOffset", agencyTimeZoneOffset, 0)
                .addNum("routeType", routeType)
                .addEntityId("agencyId", getAgency())
                .addEntityId("routeId", getRoute())
                .addEntityId("tripId", trip)
                .addStr("headsign", headsign)
                .addObj("serviceDate", serviceDate)
                .addStr("routeBrandingUrl", routeBrandingUrl)
                .addCol("intermediateStops", intermediateStops)
                .addObj("legGeometry", legGeometry)
                .addCol("walkSteps", walkSteps)
                .addCol("streetNotes", streetNotes)
                .addCol("transitAlerts", transitAlerts)
                .addStr("boardRule", boardRule)
                .addStr("alightRule", alightRule)
                .addBool("walkingBike", walkingBike)
                .addBool("rentedVehicle", rentedVehicle)
                .addStr("bikeRentalNetwork", vehicleRentalNetwork)
                .addObj("transferFromPrevLeg", transferFromPrevLeg)
                .addObj("transferToNextLeg", transferToNextLeg)
                .toString();
    }

    /**
     * The mode (e.g., <code>Walk</code>) used when traversing this leg.
     */
    public TraverseMode getMode() {
        return mode;
    }

    /**
     * The date and time this leg begins.
     */
    public Calendar getStartTime() {
        return startTime;
    }

    public void setStartTime(Calendar startTime) {
        this.startTime = startTime;
    }

    /**
     * The date and time this leg ends.
     */
    public Calendar getEndTime() {
        return endTime;
    }

    public void setEndTime(Calendar endTime) {
        this.endTime = endTime;
    }

    /**
     * For transit leg, the offset from the scheduled departure-time of the boarding stop in this
     * leg. "scheduled time of departure at boarding stop" = startTime - departureDelay Unit:
     * seconds.
     */
    public int getDepartureDelay() {
        return departureDelay;
    }

    public void setDepartureDelay(int departureDelay) {
        this.departureDelay = departureDelay;
    }

    /**
     * For transit leg, the offset from the scheduled arrival-time of the alighting stop in this
     * leg. "scheduled time of arrival at alighting stop" = endTime - arrivalDelay Unit: seconds.
     */
    public int getArrivalDelay() {
        return arrivalDelay;
    }

    public void setArrivalDelay(int arrivalDelay) {
        this.arrivalDelay = arrivalDelay;
    }

    /**
     * Whether there is real-time data about this Leg
     */
    public Boolean getRealTime() {
        return realTime;
    }

    public void setRealTime(Boolean realTime) {
        this.realTime = realTime;
    }

    /**
     * Whether this Leg describes a flexible trip. The reason we need this is that FlexTrip does not
     * inherit from Trip, so that the information that the Trip is flexible would be lost when
     * creating this object.
     */
    public boolean isFlexibleTrip() {
        return flexibleTrip;
    }

    public void setFlexibleTrip(boolean flexibleTrip) {
        this.flexibleTrip = flexibleTrip;
    }

    /**
     * Is this a frequency-based trip with non-strict departure times?
     */
    public Boolean getNonExactFrequency() {
        return isNonExactFrequency;
    }

    public void setNonExactFrequency(Boolean nonExactFrequency) {
        isNonExactFrequency = nonExactFrequency;
    }

    /**
     * The best estimate of the time between two arriving vehicles. This is particularly important
     * for non-strict frequency trips, but could become important for real-time trips, strict
     * frequency trips, and scheduled trips with empirical headways.
     */
    public Integer getHeadway() {
        return headway;
    }

    public void setHeadway(Integer headway) {
        this.headway = headway;
    }

    /**
     * The distance traveled while traversing the leg in meters.
     */
    public Double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    /**
     * Is this leg a traversing pathways?
     */
    public Boolean getPathway() {
        return pathway;
    }

    public void setPathway(Boolean pathway) {
        this.pathway = pathway;
    }

    /**
     * The GTFS pathway id
     */
    public FeedScopedId getPathwayId() {
        return pathwayId;
    }

    public void setPathwayId(FeedScopedId pathwayId) {
        this.pathwayId = pathwayId;
    }

    public int getAgencyTimeZoneOffset() {
        return agencyTimeZoneOffset;
    }

    public void setAgencyTimeZoneOffset(int agencyTimeZoneOffset) {
        this.agencyTimeZoneOffset = agencyTimeZoneOffset;
    }

    /**
     * For transit legs, the type of the route. Non transit -1 When 0-7: 0 Tram, 1 Subway, 2 Train,
     * 3 Bus, 4 Ferry, 5 Cable Car, 6 Gondola, 7 Funicular When equal or highter than 100, it is
     * coded using the Hierarchical Vehicle Type (HVT) codes from the European TPEG standard Also
     * see http://groups.google.com/group/gtfs-changes/msg/ed917a69cf8c5bef
     */
    public Integer getRouteType() {
        return routeType;
    }

    public void setRouteType(Integer routeType) {
        this.routeType = routeType;
    }

    /**
     * For transit legs, the headsign of the bus or train being used. For non-transit legs, null.
     */
    public String getHeadsign() {
        return headsign;
    }

    public void setHeadsign(String headsign) {
        this.headsign = headsign;
    }

    /**
     * For transit legs, the service date of the trip. For non-transit legs, null.
     * <p>
     * The trip service date should be used to identify the correct trip schedule and can not be
     * trusted to display the date for any departures or arrivals. For example, the first departure
     * for a given trip may happen at service date March 25th and service time 25:00, which in local
     * time would be Mach 26th 01:00.
     */
    public ServiceDate getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(ServiceDate serviceDate) {
        this.serviceDate = serviceDate;
    }

    /**
     * For transit leg, the route's branding URL (if one exists). For non-transit legs, null.
     */
    public String getRouteBrandingUrl() {
        return routeBrandingUrl;
    }

    public void setRouteBrandingUrl(String routeBrandingUrl) {
        this.routeBrandingUrl = routeBrandingUrl;
    }

    /**
     * The Place where the leg originates.
     */
    public Place getFrom() {
        return from;
    }

    public void setFrom(Place from) {
        this.from = from;
    }

    /**
     * The Place where the leg begins.
     */
    public Place getTo() {
        return to;
    }

    public void setTo(Place to) {
        this.to = to;
    }

    /**
     * For transit legs, intermediate stops between the Place where the leg originates and the Place
     * where the leg ends. For non-transit legs, {@code null}. This field is optional i.e. it is
     * always {@code null} unless {@code showIntermediateStops} parameter is set to "true" in the
     * planner request.
     */
    public List<StopArrival> getIntermediateStops() {
        return intermediateStops;
    }

    public void setIntermediateStops(List<StopArrival> intermediateStops) {
        this.intermediateStops = intermediateStops;
    }

    /**
     * The leg's geometry.
     */
    public EncodedPolylineBean getLegGeometry() {
        return legGeometry;
    }

    public void setLegGeometry(EncodedPolylineBean legGeometry) {
        this.legGeometry = legGeometry;
    }

    /**
     * A series of turn by turn instructions used for walking, biking and driving.
     */
    public List<WalkStep> getWalkSteps() {
        return walkSteps;
    }

    public void setWalkSteps(List<WalkStep> walkSteps) {
        this.walkSteps = walkSteps;
    }

    public Set<StreetNote> getStreetNotes() {
        return streetNotes;
    }

    public void setStreetNotes(Set<StreetNote> streetNotes) {
        this.streetNotes = streetNotes;
    }

    public Set<TransitAlert> getTransitAlerts() {
        return transitAlerts;
    }

    public void setTransitAlerts(Set<TransitAlert> transitAlerts) {
        this.transitAlerts = transitAlerts;
    }

    public String getBoardRule() {
        return boardRule;
    }

    public void setBoardRule(String boardRule) {
        this.boardRule = boardRule;
    }

    public String getAlightRule() {
        return alightRule;
    }

    public void setAlightRule(String alightRule) {
        this.alightRule = alightRule;
    }

    public BookingInfo getDropOffBookingInfo() {
        return dropOffBookingInfo;
    }

    public void setDropOffBookingInfo(BookingInfo dropOffBookingInfo) {
        this.dropOffBookingInfo = dropOffBookingInfo;
    }

    public BookingInfo getPickupBookingInfo() {
        return pickupBookingInfo;
    }

    public void setPickupBookingInfo(BookingInfo pickupBookingInfo) {
        this.pickupBookingInfo = pickupBookingInfo;
    }

    public ConstrainedTransfer getTransferFromPrevLeg() {
        return transferFromPrevLeg;
    }

    public void setTransferFromPrevLeg(ConstrainedTransfer transferFromPrevLeg) {
        this.transferFromPrevLeg = transferFromPrevLeg;
    }

    public ConstrainedTransfer getTransferToNextLeg() {
        return transferToNextLeg;
    }

    public void setTransferToNextLeg(ConstrainedTransfer transferToNextLeg) {
        this.transferToNextLeg = transferToNextLeg;
    }

    public Integer getBoardStopPosInPattern() {
        return boardStopPosInPattern;
    }

    public void setBoardStopPosInPattern(Integer boardStopPosInPattern) {
        this.boardStopPosInPattern = boardStopPosInPattern;
    }

    public Integer getAlightStopPosInPattern() {
        return alightStopPosInPattern;
    }

    public void setAlightStopPosInPattern(Integer alightStopPosInPattern) {
        this.alightStopPosInPattern = alightStopPosInPattern;
    }

    public Integer getBoardingGtfsStopSequence() {
        return boardingGtfsStopSequence;
    }

    public void setBoardingGtfsStopSequence(Integer boardingGtfsStopSequence) {
        this.boardingGtfsStopSequence = boardingGtfsStopSequence;
    }

    public Integer getAlightGtfsStopSequence() {
        return alightGtfsStopSequence;
    }

    public void setAlightGtfsStopSequence(Integer alightGtfsStopSequence) {
        this.alightGtfsStopSequence = alightGtfsStopSequence;
    }

    /**
     * Is this leg walking with a bike?
     */
    public Boolean getWalkingBike() {
        return walkingBike;
    }

    public void setWalkingBike(Boolean walkingBike) {
        this.walkingBike = walkingBike;
    }

    public Boolean getRentedVehicle() {
        return rentedVehicle;
    }

    public void setRentedVehicle(Boolean rentedVehicle) {
        this.rentedVehicle = rentedVehicle;
    }

    public String getVehicleRentalNetwork() {
        return vehicleRentalNetwork;
    }

    /**
     * If a generalized cost is used in the routing algorithm, this should be the "delta" cost
     * computed by the algorithm for the section this leg account for. This is relevant for anyone
     * who want to debug an search and tuning the system. The unit should be equivalent to the cost
     * of "one second of transit".
     * <p>
     * -1 indicate that the cost is not set/computed.
     */
    public int getGeneralizedCost() {
        return generalizedCost;
    }

    public void setGeneralizedCost(int generalizedCost) {
        this.generalizedCost = generalizedCost;
    }
}
