/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareAttribute;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.WrappedCurrency;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.spt.GraphPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A set of edges on a single route
 * */
class Ride {
    AgencyAndId route;

    Set<String> zones;

    String startZone;

    String endZone;

    long startTime;

    long endTime;

    //generic classifier and start/end stops, unused in DefaultFareServiceImpl but maybe useful elsewhere 
	public Object classifier;
	public Stop firstStop; 
	public Stop lastStop;

    public Ride() {
        zones = new HashSet<String>();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Ride");
        if (startZone != null) {
            builder.append("(from ");
            builder.append(startZone);
        }
        if (endZone != null) {
            builder.append(" to ");
            builder.append(endZone);
        }
        builder.append(" on ");
        builder.append(route);
        if (zones.size() > 0) {
            builder.append(" through ");
            boolean first = true;
            for (String zone : zones) {
                if (first) {
                    first = false;
                } else {
                    builder.append(",");
                }
                builder.append(zone);
            }
        }
        builder.append(" at ");
        builder.append(startTime);
        if (classifier != null) {
        	builder.append(", classified by ");
        	builder.append(classifier.toString());
        }
        builder.append(")");
        return builder.toString();
    }
}


/**
 * This fare service impl handles the cases that GTFS handles.  For more interesting 
 * fare structures, like New York's MTA, you get to implement your own FareService. 
 * See this thread on gtfs-changes explaining the proper interpretation of fares.txt: 
 * http://groups.google.com/group/gtfs-changes/browse_thread/thread/8a4a48ae1e742517/4f81b826cb732f3b
*/
public class DefaultFareServiceImpl implements FareService, Serializable {
	private static final long serialVersionUID = 1L;

	private static final Logger _log = LoggerFactory.getLogger(DefaultFareServiceImpl.class);

	private HashMap<AgencyAndId, FareRuleSet> fareRules;
	private HashMap<AgencyAndId, FareAttribute> fareAttributes;

	public DefaultFareServiceImpl(HashMap<AgencyAndId, FareRuleSet> fareRules,
			HashMap<AgencyAndId, FareAttribute> fareAttributes) {
		this.fareRules = fareRules;
		this.fareAttributes = fareAttributes;
	}

	@Override
	public Fare getCost(GraphPath path) {

    	LinkedList<State> states = path.states;

        Currency currency = null;

        // create rides
        List<Ride> rides = new ArrayList<Ride>();
        Ride newRide = null;
        for (State curr : states) {
        	EdgeNarrative edgeNarrative = curr.getBackEdgeNarrative();
        	/* skip initial state, which has no back edges */
            if (edgeNarrative == null)
                continue;

            String zone = curr.getZone();
            AgencyAndId route = curr.getRoute();
            TraverseMode mode = edgeNarrative.getMode();
            if (zone == null && route == null) {
                newRide = null;
            } else {
                if (mode.isTransit() || mode == TraverseMode.BOARDING){
                  if (newRide == null || !route.equals(newRide.route)) {
                      newRide = new Ride();
                      rides.add(newRide);
                      newRide.startZone = zone;
                      newRide.route = route;
                      newRide.startTime = curr.getTime();
                  }
                  newRide.zones.add(zone);
                  newRide.endZone = zone;
                  newRide.endTime = curr.getTime();
            	}
            }
        }

        // There are no rides, so there's no fare.
        if (rides.size() == 0) {
            return null;
        }

        // greedily consume rides

        Set<String> zones = new HashSet<String>();
        Set<AgencyAndId> routes = new HashSet<AgencyAndId>();
        String startZone = null;
        int transfersUsed = -1;
        float totalFare = 0, currentFare = -1;
        long startTime = rides.get(0).startTime;
        for (int i = 0; i < rides.size(); ++i) {
            Ride ride = rides.get(i);
            if (startZone == null) {
                startZone = ride.startZone;
            }
            float bestFare = Float.MAX_VALUE;
            routes.add(ride.route);
            zones.addAll(ride.zones);
            transfersUsed += 1;

            long tripTime = ride.startTime - startTime;
            // find the best fare that matches this set of rides
            for (AgencyAndId fareId : fareAttributes.keySet()) {
                FareRuleSet ruleSet = fareRules.get(fareId);
                if (ruleSet==null || ruleSet.matches(startZone, ride.endZone, zones, routes)) {
                    FareAttribute attribute = fareAttributes.get(fareId);
                    if (attribute.isTransfersSet() && attribute.getTransfers() < transfersUsed) {
                        continue;
                    }
                    // assume transfers are evaluated at boarding time,
                    // as trimet does
                    if (attribute.isTransferDurationSet()
                            && tripTime > attribute.getTransferDuration()) {
                        continue;
                    }
                    float newFare = attribute.getPrice();
                    if (newFare < bestFare) {
                        bestFare = newFare;
                        currency = Currency.getInstance(attribute.getCurrencyType());
                    }
                }
            }

            if (bestFare == Float.MAX_VALUE) {
                if (currentFare == -1) {
                    // Problem: there's no fare for this ride.
                    _log.warn("No fare for a perfectly good ride: " + ride);
                    return null;
                }

                // there's no fare, but we can fall back to the previous fare, and retry starting
                // here
                totalFare += currentFare;
                currentFare = -1;
                transfersUsed = -1;
                --i;
                zones = new HashSet<String>();
                startZone = ride.startZone;
                routes = new HashSet<AgencyAndId>();
                startTime = ride.startTime;
            } else {
                currentFare = bestFare;
            }
        }

        totalFare += currentFare;

        Fare fare = new Fare();
        fare.addFare(FareType.regular, new WrappedCurrency(currency),
                (int) Math.round(totalFare * Math.pow(10, currency.getDefaultFractionDigits())));
        return fare;
	}

}