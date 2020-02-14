package org.opentripplanner.model.plan;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.util.model.EncodedPolylineBean;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
   public Double distanceMeters = null;

   /**
    * Is this leg a traversing pathways?
    */
   public Boolean pathway = false;

   /**
    * The mode (e.g., <code>Walk</code>) used when traversing this leg.
    */
   public TraverseMode mode = TraverseMode.WALK;

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
   public FeedScopedId routeId = null;

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
   public FeedScopedId tripId = null;

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
   public Place from = null;

   /**
    * The Place where the leg begins.
    */
   public Place to = null;

   /**
    * For transit legs, intermediate stops between the Place where the leg originates and the Place where the leg ends.
    * For non-transit legs, null.
    * This field is optional i.e. it is always null unless "showIntermediateStops" parameter is set to "true" in the planner request.
    */
   public List<Place> intermediateStops;

   /**
    * The leg's geometry.
    */
   public EncodedPolylineBean legGeometry;

   /**
    * A series of turn by turn instructions used for walking, biking and driving.
    */
   public List<WalkStep> walkSteps;

   public Set<Alert> alerts = new HashSet<>();

   public List<AlertPatch> alertPatches = new ArrayList<>();

   public String routeShortName;

   public String routeLongName;

   public String boardRule;

   public String alightRule;

   public Boolean rentedBike;

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

    /**
    * The leg's duration in seconds
    */
    public long getDuration() {
        // Round to closest second; Hence subtract 500 ms before dividing by 1000
        return (500 + endTime.getTimeInMillis() - startTime.getTimeInMillis()) / 1000;
    }

    public void addAlert(Alert alert) {
        alerts.add(alert);
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

    public void addAlertPatch(AlertPatch alertPatch) {
        if (!alertPatches.contains(alertPatch)) {
            alertPatches.add(alertPatch);
        }
    }

    /**
     * Compare to legs to determine if they start and end at the same place and time.
     *
     * Note! Properties like mode and trip is NOT considered.
     */
    public boolean sameStartAndEnd(Leg other) {
        if (this == other) { return true; }
        return startTime.equals(other.startTime)
                && endTime.equals(other.endTime)
                && from.sameLocation(other.from)
                && to.sameLocation(other.to);
    }

    /** Should be used for debug logging only */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toStringAdd(sb, ", from", from);
        toStringAdd(sb, ", to", to);
        toStringAddCal(sb, ", startTime", startTime);
        toStringAddCal(sb, ", endTime", endTime);
        toStringAddPosInt(sb, ", departureDelay", departureDelay);
        toStringAddPosInt(sb, ", arrivalDelay", arrivalDelay);
        toStringAdd(sb, ", realTime", realTime);
        toStringAdd(sb, ", isNonExactFrequency", isNonExactFrequency);
        toStringAdd(sb, ", headway", headway);
        toStringAdd(sb, ", distance", distanceMeters);
        toStringAdd(sb, ", pathway", pathway);
        toStringAdd(sb, ", mode", mode);
        toStringAdd(sb, ", route", route);
        toStringAdd(sb, ", agencyName", agencyName);
        toStringAdd(sb, ", agencyUrl", agencyUrl);
        toStringAdd(sb, ", agencyBrandingUrl", agencyBrandingUrl);
        toStringAddPosInt(sb, ", agencyTimeZoneOffset", agencyTimeZoneOffset);
        toStringAdd(sb, ", routeColor", routeColor);
        toStringAdd(sb, ", routeType", routeType);
        toStringAdd(sb, ", routeId", routeId);
        toStringAdd(sb, ", routeTextColor", routeTextColor);
        toStringAdd(sb, ", interlineWithPreviousLeg", interlineWithPreviousLeg);
        toStringAdd(sb, ", tripShortName", tripShortName);
        toStringAdd(sb, ", tripBlockId", tripBlockId);
        toStringAdd(sb, ", headsign", headsign);
        toStringAdd(sb, ", agencyId", agencyId);
        toStringAdd(sb, ", tripId", tripId);
        toStringAdd(sb, ", serviceDate", serviceDate);
        toStringAdd(sb, ", routeBrandingUrl", routeBrandingUrl);
        toStringAdd(sb, ", intermediateStops", intermediateStops);
        toStringAdd(sb, ", legGeometry", legGeometry);
        toStringAdd(sb, ", walkSteps", walkSteps);
        toStringAdd(sb, ", alerts", alerts);
        toStringAdd(sb, ", alertPatches", alertPatches);
        toStringAdd(sb, ", routeShortName", routeShortName);
        toStringAdd(sb, ", routeLongName", routeLongName);
        toStringAdd(sb, ", boardRule", boardRule);
        toStringAdd(sb, ", alightRule", alightRule);
        toStringAdd(sb, ", rentedBike", rentedBike);
        return "Leg{" + (sb.length() > 0 ? sb.substring(2) : "") + "}";
    }

    private static void toStringAdd(StringBuilder sb, String name, Boolean value) {
        if(value == null || !value) { return; }
        sb.append(name);
    }

    private static void toStringAdd(StringBuilder sb, String name, String value) {
        if(value == null) { return; }
        sb.append(name).append("='").append(value).append("'");
    }

    private static void toStringAddCal(StringBuilder sb, String name, Calendar value) {
        if(value == null) { return; }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd mm:hh:ssZZZ");
        sb.append(name).append("='").append(sdf.format(value.getTime())).append("'");
    }

    private static void toStringAdd(StringBuilder sb, String name, Object value) {
        if(value == null) { return; }
        sb.append(name).append("=").append(value);
    }

    private static void toStringAddPosInt(StringBuilder sb, String name, int value) {
        if(value == 0) { return; }
        sb.append(name);
    }
}
