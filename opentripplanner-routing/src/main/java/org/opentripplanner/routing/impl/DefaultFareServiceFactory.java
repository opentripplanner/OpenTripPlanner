package org.opentripplanner.routing.impl;

import java.util.Collection;
import java.util.HashMap;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareAttribute;
import org.onebusaway.gtfs.model.FareRule;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.FareServiceFactory;

public class DefaultFareServiceFactory implements FareServiceFactory {
    private HashMap<AgencyAndId, FareRuleSet> fareRules;
    HashMap<AgencyAndId, FareAttribute> fareAttributes;
    
	public FareService makeFareService() {
		return new DefaultFareServiceImpl(fareRules, fareAttributes); 
	}

    private HashMap<AgencyAndId, FareRuleSet> getFareRules(GtfsRelationalDao dao) {
        if (fareRules == null) {
            fareRules = new HashMap<AgencyAndId, FareRuleSet>();
            Collection<FareRule> rules = dao.getAllFareRules();
            for (FareRule rule: rules) {
                FareAttribute fare = rule.getFare();
                AgencyAndId id = fare.getId();
                FareRuleSet fareRule = fareRules.get(id);
                if (fareRule == null) {
                    fareRule = new FareRuleSet();
                    fareRules.put(id, fareRule);
                }
                String contains = rule.getContainsId();
                if (contains != null) {
                    fareRule.addContains(contains);
                }
                String origin = rule.getOriginId();
                String destination = rule.getDestinationId();
                if (origin != null || destination != null) {
                    fareRule.addOriginDestination(origin, destination);
                }
                Route route = rule.getRoute();
                if (route != null) {
                    AgencyAndId routeId = route.getId();
                    fareRule.addRoute(routeId);
                }
            }
        }
        return fareRules;
    }
    
	@Override
	public void setDao(GtfsRelationalDao dao) {
	    
        fareRules = getFareRules(dao);
        fareAttributes = new HashMap<AgencyAndId, FareAttribute>(); 
        for (AgencyAndId fareId: fareRules.keySet()) {
            FareAttribute attribute = dao.getFareAttributeForId(fareId);
            fareAttributes.put(fareId, attribute);
        }
		
	}
}
