package org.opentripplanner.api.model;

import org.opentripplanner.api.model.alertpatch.ApiAlert;
import org.opentripplanner.util.model.EncodedPolylineBean;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

 /**
 * One leg of a trip -- that is, a temporally continuous piece of the journey that takes place on a
 * particular vehicle (or on foot).
 */

public class ApiLeg {

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
    public Double distance = null;

    /**
     * Is this leg a traversing pathways?
     */
    public Boolean pathway = false;

    /**
     * The mode (e.g., <code>Walk</code>) used when traversing this leg.
     */
    public String mode;

     /** Whether this leg is a transit leg or not. */
    public Boolean transitLeg;

    /**
     * For transit legs, the route of the bus or train being used. For non-transit legs, the name of
     * the street being traversed.
     */
    public String route = "";

    public String agencyName;

    public String agencyUrl;

    public String agencyBrandingUrl;

    public int agencyTimeZoneOffset;

    /**
     * For transit leg, the route's (background) color (if one exists). For non-transit legs, null.
     */
    public String routeColor = null;

    /**
     * For transit legs, the type of the route. Non transit -1
     * When 0-7: 0 Tram, 1 Subway, 2 Train, 3 Bus, 4 Ferry, 5 Cable Car, 6 Gondola, 7 Funicular
     * When equal or highter than 100, it is coded using the Hierarchical Vehicle Type (HVT) codes from the European TPEG standard
     * Also see http://groups.google.com/group/gtfs-changes/msg/ed917a69cf8c5bef
     */
    public Integer routeType = null;

    /**
     * For transit legs, the ID of the route.
     * For non-transit legs, null.
     */
    public String routeId = null;

    /**
     * For transit leg, the route's text color (if one exists). For non-transit legs, null.
     */
    public String routeTextColor = null;

    /**
     * For transit legs, if the rider should stay on the vehicle as it changes route names.
     */
    public Boolean interlineWithPreviousLeg;


    /**
     * For transit leg, the trip's short name (if one exists). For non-transit legs, null.
     */
    public String tripShortName = null;

    /**
     * For transit leg, the trip's block ID (if one exists). For non-transit legs, null.
     */
    public String tripBlockId = null;

    /**
     * For transit legs, the headsign of the bus or train being used. For non-transit legs, null.
     */
    public String headsign = null;

    /**
     * For transit legs, the ID of the transit agency that operates the service used for this leg.
     * For non-transit legs, null.
     */
    public String agencyId = null;

    /**
     * For transit legs, the ID of the trip.
     * For non-transit legs, null.
     */
    public String tripId = null;

    /**
     * For transit legs, the service date of the trip.
     * For non-transit legs, null.
     */
    public String serviceDate = null;

     /**
      * For transit leg, the route's branding URL (if one exists). For non-transit legs, null.
      */
     public String routeBrandingUrl = null;

     /**
     * The Place where the leg originates.
     */
    public ApiPlace from = null;

    /**
     * The Place where the leg begins.
     */
    public ApiPlace to = null;

    /**
     * For transit legs, intermediate stops between the Place where the leg originates and the Place where the leg ends.
     * For non-transit legs, null.
     * This field is optional i.e. it is always null unless "showIntermediateStops" parameter is set to "true" in the planner request.
     */
    public List<ApiPlace> intermediateStops;

    /**
     * The leg's geometry.
     */
    public EncodedPolylineBean legGeometry;

    /**
     * A series of turn by turn instructions used for walking, biking and driving.
     */
    public List<ApiWalkStep> steps;

    public List<ApiAlert> alerts;

    public String routeShortName;

    public String routeLongName;

    public String boardRule;

    public String alightRule;

    public Boolean rentedBike;

    /**
     * The leg's duration in seconds
     */
    public double getDuration() {
        return endTime.getTimeInMillis()/1000.0 - startTime.getTimeInMillis()/1000.0;
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
 }
