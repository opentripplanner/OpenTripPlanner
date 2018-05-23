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
        List<ItinerarySummary> summaries = new LinkedList<>();
        long bestNonTransitTime = Long.MAX_VALUE;

        LOG.debug("Filtering ...\n");

        for (Itinerary i : plan.itinerary) {
            ItinerarySummary s = new ItinerarySummary(i);
            summaries.add(s);
            if(!s.hasTransit && i.walkTime < bestNonTransitTime) {
                bestNonTransitTime = i.walkTime;
            }
            LOG.info("summary: transit, walk, flight " +
                     String.valueOf(s.regularTransitTime) + " " +
                     String.valueOf(i.walkTime) + " " +
                     String.valueOf(s.flightTime)
                     );
        }

        for (ItinerarySummary summary : summaries) {
            // If this is a transit option whose walk/bike time is greater than
            // that of the walk/bike-only option, do not include in plan
            if(summary.hasTransit && summary.i.walkTime > bestNonTransitTime) {
                summary.remove = true;
                LOG.info("remove summary");
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
    public long duration;
    public long startTime = 0;
    public long endTime = 0;
    public long walkTime = 0;
    public long regularTransitTime = 0;
    public long flightTime = 0;

    public ItinerarySummary(Itinerary itin) {
        this.i = itin;

        Iterator<Leg> it = itin.legs.iterator();
        while (it.hasNext()) {
            Leg leg = it.next();
            if (leg.isTransitLeg()) {
                this.hasTransit = true;
                if (leg.mode.equals(TraverseMode.AIRPLANE.toString())) {
                    this.flightTime += leg.getDuration();
                } else {
                    this.regularTransitTime += leg.getDuration();
                }
            }
        }
    }
}

