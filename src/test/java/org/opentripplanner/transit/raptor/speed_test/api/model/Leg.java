package org.opentripplanner.transit.raptor.speed_test.api.model;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.Calendar;
import java.util.List;

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
    * The distance traveled while traversing the leg in meters.
    */
   public Double distance = null;

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
    * The Place where the leg originates.
    */
   public PlaceAPI from = null;

   /**
    * The Place where the leg begins.
    */
   public PlaceAPI to = null;

   /**
    * For transit legs, intermediate stops between the Place where the leg originates and the Place where the leg ends.
    * For non-transit legs, null.
    * This field is optional i.e. it is always null unless "showIntermediateStops" parameter is set to "true" in the planner request.
    */
   public List<PlaceAPI> stop;

   public String routeShortName;

   public String routeLongName;

    /**
    * Whether this leg is a transit leg or not.
    * @return Boolean true if the leg is a transit leg
    */
   public Boolean isTransitLeg() {
      return mode.isTransit();
   }

   /**
    * Whether this leg is a transit leg or not.
    * @return Boolean true if the leg is a transit leg
    */
   public boolean isWalkLeg() {
      return mode.isWalking();
   }
   /**
    * The leg's duration in seconds
    */
   public double getDuration() {
       return (500 + endTime.getTimeInMillis() - startTime.getTimeInMillis())/1000.0;
   }
}
