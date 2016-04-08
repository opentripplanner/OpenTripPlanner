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
import org.onebusaway.gtfs.model.FareAttribute;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.services.FareService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

public class SeattleFareServiceFactory extends DefaultFareServiceFactory {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(SeattleFareServiceFactory.class);

    private Map<AgencyAndId, FareRuleSet> youthFareRules = new HashMap<AgencyAndId, FareRuleSet>();

    private Map<AgencyAndId, FareRuleSet> seniorFareRules = new HashMap<AgencyAndId, FareRuleSet>();
  
    @Override
    public FareService makeFareService() {
    	
    	// For each fare attribute, add a duplicate fare attribute for alternate fare classes.
    	for (FareRuleSet rule : regularFareRules.values()) {
    		FareAttribute fa = rule.getFareAttribute();
			addMissingFare(youthFareRules, rule, fa.getYouthPrice());
			addMissingFare(seniorFareRules, rule, fa.getSeniorPrice());
    	}
    	    
    	SeattleFareServiceImpl fareService = new SeattleFareServiceImpl(regularFareRules.values(),
                youthFareRules.values(), seniorFareRules.values());
    	
    	return fareService;
    }
    
    private static int internalFareId = 0;

    public void addMissingFare(Map<AgencyAndId, FareRuleSet> fareRules, FareRuleSet fareRule, float price) {
    
    	String feedId = fareRule.getFareAttribute().getId().getAgencyId();
    	String agencyId = fareRule.getAgency();
    	
    	FareAttribute fare = createInternalFareAttribute(price, feedId);       
    	FareRuleSet newFareRule = new FareRuleSet(fare);
        newFareRule.setAgency(agencyId);
        
        for (P2<String> originDestZone : fareRule.getOriginDestinations()) {
            newFareRule.addOriginDestination(originDestZone.first, originDestZone.second);
        }
        
        for (AgencyAndId route : fareRule.getRoutes())
            newFareRule.addRoute(route);
        
        fareRules.put(fare.getId(), newFareRule);
    }
    
    private FareAttribute createInternalFareAttribute(float price, String id) {
        FareAttribute fare = new FareAttribute();
        fare.setTransferDuration(SeattleFareServiceImpl.TRANSFER_DURATION_SEC);
        fare.setCurrencyType("USD");
        fare.setPrice(price);
        fare.setId(new AgencyAndId(id, "internal_"
                + internalFareId));
        internalFareId++;
        return fare;
    }
    
    @Override
    public void configure(JsonNode config) {
        // No config for the moment
    }
}
