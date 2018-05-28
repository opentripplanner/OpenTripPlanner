/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.api.resource;

import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import org.opentripplanner.api.model.*;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.RoutingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A library class for filtering itineraries in a TripPlan object.
   Motivation: not all itineraries found by a clean computational algorithm
   make sense to a human traveler. Instead of trying to add complexity
   to the search itself, we try to drop unwanted items in a post filtering stage.
 */

public abstract class TripPlanFilter {

    public static final Logger LOG = LoggerFactory.getLogger(TripPlanFilter.class);

    /**
     * Generates an optimized trip plan from original plan
     */
    public static TripPlan filterPlan(TripPlan plan, RoutingRequest request) {
        if (request.itineraryFiltering < 0.01) { // no more effect at this level
            return plan;
        }
        LOG.debug("Filtering with factor " + String.valueOf(request.itineraryFiltering));

        List<ItinerarySummary> summaries = new LinkedList<>();
        long bestNonTransitTime = Long.MAX_VALUE;
        double tolerance = 90; // minimal significant time loss in seconds

        double filtering = 1 + 1/request.itineraryFiltering; // range starts from 1
        double fullItinFiltering = Math.sqrt(filtering); // for comparing total itinerary duration
        double base1 = tolerance/filtering; // allowed decrease
        double base2 = 2*tolerance*filtering; // minimal increase for dropping

        // Collect the required summary info
        for (Itinerary i : plan.itinerary) {
            ItinerarySummary s = new ItinerarySummary(i);
            summaries.add(s);
            if(!s.hasTransit && i.walkTime < bestNonTransitTime) {
                bestNonTransitTime = i.walkTime;
            }
        }

        // Filter 1: transit option whose walk/bike time is greater than
        // that of the walk/bike-only option, do not include in plan
        for (ItinerarySummary summary : summaries) {
            if(summary.hasTransit && summary.i.walkTime > bestNonTransitTime) {
                summary.remove = true;
                LOG.debug("remove summary with unnecessary transit leg");
            }
        }

        // Filter 2: bad itinerary is not better in any respect
        // and it includes walk, transit or flight sections which are really much worse
        // than another itinerary
        for (ItinerarySummary poor : summaries) {
            if (poor.remove)
                continue;
            for (ItinerarySummary good : summaries) {
                if (poor == good)
                    continue;
                if (poor.startTime <= good.startTime && // leaves earlier
                    poor.endTime >= good.endTime && // arrives later
                    poor.i.transfers >= good.i.transfers && // does not reduce transfers
                    // check that all modes are at least almost as good
                    poor.i.walkTime + base1 > good.i.walkTime && // does not reduce much walking
                    poor.flightTime + base1 > good.flightTime && // does not reduce much flying. Keeps train vs flight!
                    // note: filtered item may have less transit time because transit is preferred over walking!
                    // check if some mode is considerably worse
                    (   poor.i.walkTime > filtering*good.i.walkTime + base2 || // much more walking
                        poor.regularTransitTime > filtering*good.regularTransitTime + base2 || // much more transit
                        poor.flightTime > filtering*good.flightTime + base2 // much more flying
                    ) &&
                    // time increase in total itinerary duration must also be significant
                    // i.e. 5 min walk increase does not matter if trip takes an hour
                    poor.i.duration > fullItinFiltering*good.i.duration
                ) {
                  poor.remove = true;
                  LOG.debug("remove itinerary: \n walk=" +
                      String.valueOf(poor.i.walkTime) + " transit=" +
                      String.valueOf(poor.regularTransitTime) + " fly=" +
                      String.valueOf(poor.flightTime) + " poor \n walk=" + 
                      String.valueOf(good.i.walkTime) + " transit=" +
                      String.valueOf(good.regularTransitTime) + " fly=" +
                      String.valueOf(good.flightTime) + " good"
                  );
                }
            }
        }

        TripPlan newPlan = new TripPlan(plan.from, plan.to, plan.date);

        for (ItinerarySummary summary : summaries) {
            if(!summary.remove) {
                newPlan.addItinerary(summary.i);
            }
        }

        return newPlan;
    }
}

class ItinerarySummary {
    public Itinerary i;
    public boolean remove = false;
    public boolean hasTransit = false;
    public long startTime = 0;
    public long endTime = 0;
    public long regularTransitTime = 0;
    public long flightTime = 0;

    public ItinerarySummary(Itinerary itin) {
        this.i = itin;
        this.startTime = i.startTime.getTimeInMillis()/1000;
        this.endTime = i.endTime.getTimeInMillis()/1000;

        Iterator<Leg> it = itin.legs.iterator();
        while (it.hasNext()) {
            Leg leg = it.next();
            if (leg.isTransitLeg()) {
                this.hasTransit = true;
                if (leg.mode.equals(TraverseMode.AIRPLANE.toString())) {
                    this.flightTime += leg.getDuration();
                }
            }
        }
        // time spnt in normal transit traveling, including waiting
        this.regularTransitTime = i.duration - this.flightTime - i.walkTime;
    }
}

