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
import java.util.Collection;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareAttribute;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.WrappedCurrency;
import org.opentripplanner.routing.edgetype.HopEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.spt.GraphPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A set of edges on a single route, with associated information for calculating fares */
class Ride {
    
    String agency; // route agency

    AgencyAndId route;

    AgencyAndId trip;
    
    Set<String> zones;

    String startZone;

    String endZone;

    long startTime;

    long endTime;

    // in DefaultFareServiceImpl classifier is just the TraverseMode
    // it can be used differently in custom fare services
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
            builder.append("(from zone ");
            builder.append(startZone);
        }
        if (endZone != null) {
            builder.append(" to zone ");
            builder.append(endZone);
        }
        builder.append(" on route ");
        builder.append(route);
        if (zones.size() > 0) {
            builder.append(" through zones ");
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
 * This fare service module handles the cases that GTFS handles within a single feed.
 * It cannot necessarily handle multi-feed graphs, because a rule-less fare attribute
 * might be applied to rides on routes in another feed, for example.
 * For more interesting fare structures like New York's MTA, or cities with multiple
 * feeds and inter-feed transfer rules, you get to implement your own FareService. 
 * See this thread on gtfs-changes explaining the proper interpretation of fares.txt:
 * http://groups.google.com/group/gtfs-changes/browse_thread/thread/8a4a48ae1e742517/4f81b826cb732f3b
 */
public class DefaultFareServiceImpl implements FareService, Serializable {

    private static final long serialVersionUID = 20120229L;

    private static final Logger LOG = LoggerFactory.getLogger(DefaultFareServiceImpl.class);

    /** For each fare type (regular, student, etc...) the collection of rules that apply. */
    protected Map<FareType, Collection<FareRuleSet>> fareRulesPerType;

    public DefaultFareServiceImpl() {
        fareRulesPerType = new HashMap<>();
    }

    public void addFareRules(FareType fareType, Collection<FareRuleSet> fareRules) {
        fareRulesPerType.put(fareType, new ArrayList<>(fareRules));
    }

    protected List<Ride> createRides(GraphPath path) {
        List<Ride> rides = new LinkedList<Ride>();
        Ride ride = null;
        for (State state : path.states) {
            Edge edge = state.getBackEdge();
            if ( ! (edge instanceof HopEdge))
                continue;
            HopEdge hEdge = (HopEdge) edge;
            if (ride == null || ! state.getRoute().equals(ride.route)) {
                ride = new Ride();
                rides.add(ride);
                ride.startZone = hEdge.getBeginStop().getZoneId();
                ride.zones.add(ride.startZone);
                ride.agency = state.getBackTrip().getRoute().getAgency().getId();
                ride.route = state.getRoute();
                ride.startTime = state.getBackState().getTimeSeconds();
                ride.firstStop = hEdge.getBeginStop();
                ride.trip = state.getTripId();
            }
            ride.lastStop = hEdge.getEndStop();
            ride.endZone  = ride.lastStop.getZoneId();
            ride.zones.add(ride.endZone);
            ride.endTime  = state.getTimeSeconds();
            // in default fare service, classify rides by mode 
            ride.classifier = state.getBackMode();
        }
        return rides;
    }

    @Override
    public Fare getCost(GraphPath path) {

        List<Ride> rides = createRides(path);
        // If there are no rides, there's no fare.
        if (rides.size() == 0) {
            return null;
        }

        Fare fare = new Fare();
        boolean hasFare = false;
        for (Map.Entry<FareType, Collection<FareRuleSet>> kv : fareRulesPerType.entrySet()) {
            FareType fareType = kv.getKey();
            Collection<FareRuleSet> fareRules = kv.getValue();

            // pick up a random currency from fareAttributes,
            // we assume that all tickets use the same currency
            Currency currency = null;
            WrappedCurrency wrappedCurrency = null;
            if (fareRules.size() > 0) {
                currency = Currency.getInstance(fareRules.iterator().next().getFareAttribute()
                        .getCurrencyType());
                wrappedCurrency = new WrappedCurrency(currency);
            }

            float lowestCost = getLowestCost(fareType, rides, fareRules);

            if (lowestCost != Float.POSITIVE_INFINITY) {
                int fractionDigits = 2;
                if (currency != null)
                    fractionDigits = currency.getDefaultFractionDigits();
                int cents = (int) Math.round(lowestCost * Math.pow(10, fractionDigits));
                fare.addFare(fareType, wrappedCurrency, cents);
                hasFare = true;
            }
        }
        return hasFare ? fare : null;
    }

    protected float getLowestCost(FareType fareType, List<Ride> rides,
            Collection<FareRuleSet> fareRules) {
        // Dynamic algorithm to calculate fare cost.
        // Cell [i,j] holds the best (lowest) cost for a trip from rides[i] to rides[j]
        float[][] resultTable = new float[rides.size()][rides.size()];

        for (int i = 0; i < rides.size(); i++) {
            // each diagonal
            for (int j = 0; j < rides.size() - i; j++) {
                float cost = calculateCost(fareType, rides.subList(j, j + i + 1), fareRules);
                if (cost < 0) {
                    LOG.error("negative cost for a ride sequence");
                    cost = Float.POSITIVE_INFINITY;
                }
                resultTable[j][j + i] = cost;
                for (int k = 0; k < i; k++) {
                    float via = resultTable[j][j + k] + resultTable[j + k + 1][j + i];
                    if (resultTable[j][j + i] > via)
                        resultTable[j][j + i] = via;
                }
            }
        }
        return resultTable[0][rides.size() - 1];
    }

    protected float calculateCost(FareType fareType, List<Ride> rides,
            Collection<FareRuleSet> fareRules) {
        Set<String> zones = new HashSet<String>();
        Set<AgencyAndId> routes = new HashSet<AgencyAndId>();
        Set<String> agencies = new HashSet<String>();
        Set<AgencyAndId> trips = new HashSet<AgencyAndId>();
        int transfersUsed = -1;
        
        Ride firstRide = rides.get(0);
        long   startTime = firstRide.startTime;
        String startZone = firstRide.startZone;
        String endZone = firstRide.endZone;
        // stops don't really have an agency id, they have the per-feed default id
        String feedId = firstRide.firstStop.getId().getAgencyId();  
        long lastRideStartTime = firstRide.startTime;
        long lastRideEndTime = firstRide.endTime;
        for (Ride ride : rides) {
            if ( ! ride.firstStop.getId().getAgencyId().equals(feedId)) {
                LOG.debug("skipped multi-feed ride sequence {}", rides);
                return Float.POSITIVE_INFINITY;
            }
            lastRideStartTime = ride.startTime;
            lastRideEndTime = ride.endTime;
            endZone = ride.endZone;
            agencies.add(ride.agency);
            routes.add(ride.route);
            zones.addAll(ride.zones);
            trips.add(ride.trip);
            transfersUsed += 1;
        }
        
        FareAttribute bestAttribute = null;
        float bestFare = Float.POSITIVE_INFINITY;
        long tripTime = lastRideStartTime - startTime;
        long journeyTime = lastRideEndTime - startTime;
        	
        // find the best fare that matches this set of rides
        for (FareRuleSet ruleSet : fareRules) {
            FareAttribute attribute = ruleSet.getFareAttribute();
            // fares also don't really have an agency id, they will have the per-feed default id
            // check only if the fare is not mapped to an agency
            if (!ruleSet.hasAgencyDefined() && !attribute.getId().getAgencyId().equals(feedId))
                continue;
            
            if (ruleSet.matches(agencies, startZone, endZone, zones, routes, trips)) {
                // TODO Maybe move the code below in FareRuleSet::matches() ?
                if (attribute.isTransfersSet() && attribute.getTransfers() < transfersUsed) {
                    continue;
                }
                // assume transfers are evaluated at boarding time,
                // as trimet does
                if (attribute.isTransferDurationSet() && 
                    tripTime > attribute.getTransferDuration()) {
                    continue;
                }
                if (attribute.isJourneyDurationSet() && 
                    journeyTime > attribute.getJourneyDuration()) {
                    continue;
                }
                float newFare = attribute.getPrice();
                if (newFare < bestFare) {
                    bestAttribute = attribute;
                    bestFare = newFare;
                }
            }
        }
        LOG.debug("{} best for {}", bestAttribute, rides);
        if (bestFare == Float.POSITIVE_INFINITY) {
            LOG.debug("No fare for a ride sequence: {}", rides);
        }
        return bestFare;

    }

}