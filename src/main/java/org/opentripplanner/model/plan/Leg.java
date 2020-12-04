package org.opentripplanner.model.plan;

import org.opentripplanner.model.Agency;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.StreetNote;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.util.model.EncodedPolylineBean;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
* One leg of a trip -- that is, a temporally continuous piece of the journey that takes place on a
* particular vehicle (or on foot).
*/

public class Leg {

  /**
   * The mode (e.g., <code>Walk</code>) used when traversing this leg.
   */
  public final TraverseMode mode;


  private final Trip trip;

  /**
    * The date and time this leg begins.
    */
   public Calendar startTime = null;

   /**
    * The date and time this leg ends.
    */
   public Calendar endTime = null;

   /**
    * For transit leg, the offset from the scheduled departure-time of the boarding stop in this leg.
    * "scheduled time of departure at boarding stop" = startTime - departureDelay
    */
   public int departureDelay = 0;
   /**
    * For transit leg, the offset from the scheduled arrival-time of the alighting stop in this leg.
    * "scheduled time of arrival at alighting stop" = endTime - arrivalDelay
    */
   public int arrivalDelay = 0;

   /**
    * Whether there is real-time data about this Leg
    */
   public Boolean realTime = false;

  /**
   * Whether this Leg describes a flexible trip. The reason we need this is that FlexTrip does
   * not inherit from Trip, so that the information that the Trip is flexible would be lost when
   * creating this object.
   */
  public Boolean flexibleTrip = false;

   /**
    * Is this a frequency-based trip with non-strict departure times?
    */
   public Boolean isNonExactFrequency = null;

   /**
    * The best estimate of the time between two arriving vehicles. This is particularly important
    * for non-strict frequency trips, but could become important for real-time trips, strict
    * frequency trips, and scheduled trips with empirical headways.
    */
   public Integer headway = null;

   /**
    * The distance traveled while traversing the leg in meters.
    */
   public Double distanceMeters = null;

   /**
    * Is this leg a traversing pathways?
    */
   public Boolean pathway = false;

   public int agencyTimeZoneOffset;

   /**
    * For transit legs, the type of the route. Non transit -1
    * When 0-7: 0 Tram, 1 Subway, 2 Train, 3 Bus, 4 Ferry, 5 Cable Car, 6 Gondola, 7 Funicular
    * When equal or highter than 100, it is coded using the Hierarchical Vehicle Type (HVT) codes from the European TPEG standard
    * Also see http://groups.google.com/group/gtfs-changes/msg/ed917a69cf8c5bef
    */
   public Integer routeType = null;

   /**
    * For transit legs, if the rider should stay on the vehicle as it changes route names.
    */
   public Boolean interlineWithPreviousLeg;

   /**
    * For transit legs, the headsign of the bus or train being used. For non-transit legs, null.
    */
   public String headsign = null;

   /**
    * For transit legs, the service date of the trip.
    * For non-transit legs, null.
    * <p>
    * The trip service date should be used to identify the correct trip schedule and
    * can not be trusted to display the date for any departures or arrivals. For example,
    * the first departure for a given trip may happen at service date March 25th and
    * service time 25:00, which in local time would be Mach 26th 01:00.
    */
   public ServiceDate serviceDate = null;

    /**
     * For transit leg, the route's branding URL (if one exists). For non-transit legs, null.
     */
    public String routeBrandingUrl = null;

    /**
    * The Place where the leg originates.
    */
   public Place from = null;

   /**
    * The Place where the leg begins.
    */
   public Place to = null;

    /**
     * For transit legs, intermediate stops between the Place where the leg originates and the Place
     * where the leg ends. For non-transit legs, {@code null}. This field is optional i.e. it is
     * always {@code null} unless {@code showIntermediateStops} parameter is set to "true" in the
     * planner request.
     */
    public List<StopArrival> intermediateStops;

   /**
    * The leg's geometry.
    */
   public EncodedPolylineBean legGeometry;

   /**
    * A series of turn by turn instructions used for walking, biking and driving.
    */
   public List<WalkStep> walkSteps;

   public Set<StreetNote> streetNotes = new HashSet<>();

   public Set<TransitAlert> transitAlerts = new HashSet<>();

   public String boardRule;

   public String alightRule;

   public Boolean rentedBike;

  /**
   * If a generalized cost is used in the routing algorithm, this should be the "delta" cost
   * computed by the algorithm for the section this leg account for. This is relevant for anyone
   * who want to debug an search and tuning the system. The unit should be equivalent to the cost
   * of "one second of transit".
   * <p>
   * -1 indicate that the cost is not set/computed.
   */
  public int generalizedCost = -1;

  public Leg(TraverseMode mode) {
    if(mode.isTransit()) {
      throw new IllegalArgumentException("To create a transit leg use the other constructor.");
    }
    this.mode = mode;
    this.trip = null;
  }

  public Leg(Trip trip) {
    this.mode = TraverseMode.fromTransitMode(trip.getRoute().getMode());
    this.trip = trip;
  }

  /**
    * Whether this leg is a transit leg or not.
    * @return Boolean true if the leg is a transit leg
    */
   public Boolean isTransitLeg() {
       return mode.isTransit();
   }

    public boolean isWalkingLeg() {
        return mode.isWalking();
    }

    public boolean isOnStreetNonTransit() {
        return mode.isOnStreetNonTransit();
    }

    public boolean isFlexibleTrip() {
     return flexibleTrip;
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

    public void setTimeZone(TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTime(startTime.getTime());
        startTime = calendar;
        calendar = Calendar.getInstance(timeZone);
        calendar.setTime(endTime.getTime());
        endTime = calendar;
        agencyTimeZoneOffset = timeZone.getOffset(startTime.getTimeInMillis());
    }

    public void addAlert(TransitAlert alert) {
        transitAlerts.add(alert);
    }

    /**
     * Return {@code true} if to legs ride the same trip(same tripId) and at least part of the
     * rides overlap. Two legs overlap is they have at least one segment(from one stop to the next)
     * in common.
     */
    public boolean isPartiallySameTransitLeg(Leg other) {
      // Assert both legs are transit legs
      if(!isTransitLeg() || !other.isTransitLeg()) { throw new IllegalStateException(); }

      // Must be on the same service date
      if(!serviceDate.equals(other.serviceDate)) { return false; }

      // If NOT the same trip, return false
      if(!trip.getId().equals(other.trip.getId())) { return false; }

      // Return true if legs overlap
      return this.from.stopIndex < other.to.stopIndex && to.stopIndex > other.from.stopIndex;
    }

  /** For transit legs, the route agency. For non-transit legs {@code null}. */
  public Agency getAgency() {
    return isTransitLeg() ? getRoute().getAgency() : null;
  }

  /**
   * For transit legs, the trip operator, fallback to route operator.
   * For non-transit legs {@code null}.
   * @see Trip#getOperator()
   */
  public Operator getOperator() {
    return isTransitLeg() ? trip.getOperator() : null;
  }

  /** For transit legs, the the route. For non-transit legs, null. */
  public Route getRoute() { return isTransitLeg() ? trip.getRoute() : null; }

  /** For transit legs, the the trip. For non-transit legs, null. */
  public Trip getTrip() {  return trip; }

  /** Should be used for debug logging only */
    @Override
    public String toString() {
      return ToStringBuilder.of(Leg.class)
                .addObj("from", from)
                .addObj("to", to)
                .addCalTime("startTime", startTime)
                .addCalTime("endTime", endTime)
                .addNum("departureDelay", departureDelay, 0)
                .addNum("arrivalDelay", arrivalDelay, 0)
                .addBool("realTime", realTime)
                .addBool("isNonExactFrequency", isNonExactFrequency)
                .addNum("headway", headway)
                .addEnum("mode", mode)
                .addNum("distance", distanceMeters, "m")
                .addNum("cost", generalizedCost)
                .addBool("pathway", pathway)
                .addNum("agencyTimeZoneOffset", agencyTimeZoneOffset, 0)
                .addNum("routeType", routeType)
                .addEntityId("agencyId", getAgency())
                .addEntityId("routeId", getRoute())
                .addEntityId("tripId", trip)
                .addStr("headsign", headsign)
                .addBool("interlineWithPreviousLeg", interlineWithPreviousLeg)
                .addObj("serviceDate", serviceDate)
                .addStr("routeBrandingUrl", routeBrandingUrl)
                .addCol("intermediateStops", intermediateStops)
                .addObj("legGeometry", legGeometry)
                .addCol("walkSteps", walkSteps)
                .addCol("streetNotes", streetNotes)
                .addCol("transitAlerts", transitAlerts)
                .addStr("boardRule", boardRule)
                .addStr("alightRule", alightRule)
                .addBool("rentedBike", rentedBike)
                .toString();
    }
}
