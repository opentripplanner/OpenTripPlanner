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

/**
 * Implements the default GTFS fare rules as described in
 * http://groups.google.com/group/gtfs-changes/msg/4f81b826cb732f3b
 * 
 * @author novalis
 * 
 */
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
            for (FareRule rule : rules) {
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
        Collection<FareAttribute> fA = dao.getAllFareAttributes();
        fareAttributes = new HashMap<AgencyAndId, FareAttribute>();
        for (FareAttribute fare : fA) {
            fareAttributes.put(fare.getId(), fare);
        }

    }
}
