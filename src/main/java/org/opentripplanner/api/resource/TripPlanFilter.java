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
import java.util.ListIterator;
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
        LOG.debug("Itinerary count = " + String.valueOf(plan.itinerary.size()));

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
                LOG.debug("remove itinerary #{} with unnecessary transit leg", summaries.indexOf(summary));
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
                if (
                    (!good.hasTransit || // comparing start and end time makes sense only if transit is used
                     (poor.startTime <= good.startTime && poor.endTime >= good.endTime)) && // leaves earlier and  arrives later
                    poor.i.legs.size() >= good.i.legs.size() && // does not simplify the route
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
                  LOG.debug("remove itinerary #{}: \n walk=" +
                      String.valueOf(poor.i.walkTime) + " transit=" +
                      String.valueOf(poor.regularTransitTime) + " fly=" +
                      String.valueOf(poor.flightTime) + " poor \n walk=" +
                      String.valueOf(good.i.walkTime) + " transit=" +
                      String.valueOf(good.regularTransitTime) + " fly=" +
                      String.valueOf(good.flightTime) + " good",
                      summaries.indexOf(poor)
                  );
                }
            }
        }

        // Filter 3: remove identical non-transit itineraries
        // which show up in via point bike routing
        Iterator<ItinerarySummary> i1= summaries.iterator();
        while (i1.hasNext()) {
            ItinerarySummary s1 = i1.next();
            if (s1.hasTransit)
                continue;
            ListIterator<ItinerarySummary> i2 = summaries.listIterator(summaries.size());
            while (i2.hasPrevious()) {
                // iterate only the tail from the end to the i1 current pos
                ItinerarySummary s2 = i2.previous();
                if (s2 == s1)
                    break;
                if (s2.remove || s2.hasTransit || Math.abs(s1.i.duration - s2.i.duration) > 1)
                    continue;
                if (s1.i.legs.size() != s2.i.legs.size())
                    continue;

                Iterator<Leg> legs1 = s1.i.legs.iterator();
                Iterator<Leg> legs2 = s2.i.legs.iterator();
                boolean remove = true;
                while (legs1.hasNext() && legs2.hasNext()) {
                    Leg leg1 = legs1.next();
                    Leg leg2 = legs2.next();
                    if (
                      !leg1.mode.equals(leg2.mode) ||
                      Math.abs(leg1.getDuration() - leg2.getDuration()) > 1
                    ) {
                        remove=false;
                        break;
                    }
                }
                if (remove) {
                    s2.remove = true;
                    LOG.debug("remove duplicate non-transit itinerary #{}", summaries.indexOf(s2));
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

