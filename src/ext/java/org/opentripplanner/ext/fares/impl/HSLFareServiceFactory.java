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

package org.opentripplanner.ext.fares.impl;

import java.util.Collection;
import java.util.Map;
import org.opentripplanner.ext.fares.model.FareAttribute;
import org.opentripplanner.ext.fares.model.FareRule;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HSLFareServiceFactory extends DefaultFareServiceFactory {

  private static final Logger LOG = LoggerFactory.getLogger(HSLFareServiceImpl.class);

  @Override
  protected void fillFareRules(
    Collection<FareAttribute> fareAttributes,
    Collection<FareRule> fareRules,
    Map<FeedScopedId, FareRuleSet> fareRuleSet
  ) {
    /*
     * Create an empty FareRuleSet for each FareAttribute, as some FareAttribute may have no
     * rules attached to them.
     */
    for (FareAttribute fare : fareAttributes) {
      FeedScopedId id = fare.getId();
      FareRuleSet fareRule = fareRuleSet.get(id);
      if (fareRule == null) {
        fareRule = new FareRuleSet(fare);
        fareRuleSet.put(id, fareRule);
      }
    }

    /*
     * For each fare rule, add it to the FareRuleSet of the fare.
     */
    for (FareRule rule : fareRules) {
      FareAttribute fare = rule.getFare();
      FeedScopedId id = fare.getId();
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
      Route route = rule.getRoute();

      if (route != null) {
        FeedScopedId routeId = route.getId();
        if (origin != null && destination != null) {
          fareRule.addRouteOriginDestination(routeId.toString(), origin, destination);
        } else {
          fareRule.addRoute(routeId);
        }
      } else {
        if (origin != null || destination != null) {
          fareRule.addOriginDestination(origin, destination);
        }
      }
    }
  }

  public FareService makeFareService() {
    HSLFareServiceImpl fareService = new HSLFareServiceImpl();
    fareService.addFareRules(FareType.regular, regularFareRules.values());
    return fareService;
  }
}
