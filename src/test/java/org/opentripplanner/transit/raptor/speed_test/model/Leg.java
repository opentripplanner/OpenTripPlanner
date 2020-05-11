package org.opentripplanner.transit.raptor.speed_test.model;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.List;

/**
* One leg of a trip -- that is, a temporally continuous piece of the journey that takes place on a
* particular vehicle (or on foot).
*/

public class Leg {
   private static final int NOT_SET = -1;

   /**
    * The date and time this leg begins.
    */
   public int startTime = NOT_SET;

   /**
    * The time this leg ends.
    */
   public int endTime = NOT_SET;

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
    * For transit leg, the trip's short name (if one exists). For non-transit legs, null.
    */
   public String tripShortName = null;

   /**
    * For transit legs, the ID of the transit agency that operates the service used for this leg.
    * For non-transit legs, null.
    */
   public FeedScopedId agencyId = null;

    /**
    * The Place where the leg originates.
    */
   public Place from = null;

   /**
    * The Place where the leg begins.
    */
   public Place to = null;

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
   public int getDuration() {
       return endTime - startTime;
   }
}
