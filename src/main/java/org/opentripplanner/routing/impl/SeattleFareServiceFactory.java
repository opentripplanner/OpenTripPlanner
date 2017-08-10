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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.services.FareService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

public class SeattleFareServiceFactory extends DefaultFareServiceFactory {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(SeattleFareServiceFactory.class);

    private Multimap<String, String> agenciesByFeed = ArrayListMultimap.create();

    @Override
    public FareService makeFareService() {
    	
    	DefaultFareServiceImpl fareService = new DefaultFareServiceImpl();
    	fareService.addFareRules(FareType.regular, regularFareRules.values());
    	fareService.addFareRules(FareType.youth, regularFareRules.values());
    	fareService.addFareRules(FareType.senior, regularFareRules.values());

        fareService.setFareAdditiveStrategy(new SeattleFareAdditiveStrategy(agenciesByFeed));

    	return fareService;
    }
    
    @Override
    public void configure(JsonNode config) {
        if (config.has("useMaxFareStrategy")) {
            Iterator<JsonNode> iter = config.get("useMaxFareStrategy").iterator();
            while (iter.hasNext()) {
                // feed:agency ie 97:1
                AgencyAndId feedAndAgency = GtfsLibrary.convertIdFromString(iter.next().asText());
                String feedId = feedAndAgency.getAgencyId();
                String agencyId = feedAndAgency.getId();
                agenciesByFeed.put(feedId, agencyId);
            }
        }
    }
    
    @Override
    public void processGtfs(GtfsRelationalDao dao) {
    	// Add custom extension: trips may have a fare ID specified in KCM GTFS.
    	// Need to ensure that we are scoped to feed when adding trips to FareRuleSet,
    	// since fare IDs may not be unique across feeds and trip agency IDs
    	// may not match fare attribute agency IDs (which are feed IDs).
    	
    	Map<AgencyAndId, FareRuleSet> feedFareRules = new HashMap<>();
    	fillFareRules(null, dao.getAllFareAttributes(), dao.getAllFareRules(), feedFareRules);
    	
    	regularFareRules.putAll(feedFareRules);
    	
    	Map<String, FareRuleSet> feedFareRulesById = new HashMap<>();
            
        for (FareRuleSet rule : regularFareRules.values()) {
        	String id = rule.getFareAttribute().getId().getId();
        	feedFareRulesById.put(id, rule);
        }
        
        for (Trip trip : dao.getAllTrips()) {
        	String fareId = trip.getFareId();
        	FareRuleSet rule = feedFareRulesById.get(fareId);
        	if (rule != null)
        		rule.addTrip(trip.getId());        	
        }
        	
    }
}

class SeattleFareAdditiveStrategy implements FareAdditiveStrategy, Serializable {

    private static final long serialVersionUID = 1L;

    private Multimap<String, String> agenciesByFeed;

    SeattleFareAdditiveStrategy(Multimap<String, String> agenciesByFeed) {
        this.agenciesByFeed = agenciesByFeed;
    }

    @Override
    public float addFares(List<Ride> ride0, List<Ride> ride1, float cost0, float cost1) {
        String feedId = ride0.get(0).firstStop.getId().getAgencyId();
        String agencyId = ride0.get(0).agency;
        if (agenciesByFeed.get(feedId).contains(agencyId)) {
            for (Ride r : Iterables.concat(ride0, ride1)) {
                if (!isCorrectAgency(r, feedId, agencyId)) {
                    return cost0 + cost1;
                }
            }
            return Math.max(cost0, cost1);
        }
        return cost0 + cost1;
    }

    private static boolean isCorrectAgency(Ride r, String feedId, String agencyId) {
        String rideFeedId = r.firstStop.getId().getAgencyId();
        String rideAgencyId = r.agency;
        return feedId.equals(rideFeedId) && agencyId.equals(rideAgencyId);
    }
}
