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

import java.util.HashMap;
import java.util.Map;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.services.FareService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

public class SeattleFareServiceFactory extends DefaultFareServiceFactory {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(SeattleFareServiceFactory.class);

    @Override
    public FareService makeFareService() {
    	
    	DefaultFareServiceImpl fareService = new DefaultFareServiceImpl();
    	fareService.addFareRules(FareType.regular, regularFareRules.values());
    	fareService.addFareRules(FareType.youth, regularFareRules.values());
    	fareService.addFareRules(FareType.senior, regularFareRules.values());
    	
    	return fareService;
    }
    
    @Override
    public void configure(JsonNode config) {
        // No config for the moment
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
