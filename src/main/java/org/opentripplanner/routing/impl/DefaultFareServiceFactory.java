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
import java.util.Map;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareAttribute;
import org.onebusaway.gtfs.model.FareRule;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.FareServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the default GTFS fare rules as described in
 * http://groups.google.com/group/gtfs-changes/msg/4f81b826cb732f3b
 * 
 * @author novalis
 * 
 */
public class DefaultFareServiceFactory implements FareServiceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultFareServiceFactory.class);

    protected Map<AgencyAndId, FareRuleSet> regularFareRules = new HashMap<AgencyAndId, FareRuleSet>();

    public FareService makeFareService() {
        DefaultFareServiceImpl fareService = new DefaultFareServiceImpl();
        fareService.addFareRules(FareType.regular, regularFareRules.values());
        return fareService;
    }

    @Override
    public void setDao(GtfsRelationalDao dao) {
        fillFareRules(null, dao.getAllFareAttributes(), dao.getAllFareRules(), regularFareRules);
    }

    protected void fillFareRules(String agencyId, Collection<FareAttribute> fareAttributes,
            Collection<FareRule> fareRules, Map<AgencyAndId, FareRuleSet> fareRuleSet) {
        /*
         * Create an empty FareRuleSet for each FareAttribute, as some FareAttribute may have no
         * rules attached to them.
         */
        for (FareAttribute fare : fareAttributes) {
            AgencyAndId id = fare.getId();
            FareRuleSet fareRule = fareRuleSet.get(id);
            if (fareRule == null) {
                fareRule = new FareRuleSet(fare);
                fareRuleSet.put(id, fareRule);
                if (agencyId != null) {
                    // TODO With the new GTFS lib, use fareAttribute.agency_id directly
                    fareRule.setAgency(agencyId);
                }
            }
        }

        /*
         * For each fare rule, add it to the FareRuleSet of the fare.
         */
        for (FareRule rule : fareRules) {
            FareAttribute fare = rule.getFare();
            AgencyAndId id = fare.getId();
            FareRuleSet fareRule = fareRuleSet.get(id);
            if (fareRule == null) {
                // Should never happen by design
                LOG.error("Inexistant fare ID in fare rule: " + id);
                continue;
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
}
