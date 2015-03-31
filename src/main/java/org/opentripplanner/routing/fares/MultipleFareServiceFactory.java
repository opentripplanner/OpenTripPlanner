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

package org.opentripplanner.routing.fares;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.routing.bike_rental.TimeBasedBikeRentalFareServiceFactory;
import org.opentripplanner.routing.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.FareServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class MultipleFareServiceFactory implements FareServiceFactory {

    private static Logger log = LoggerFactory
            .getLogger(MultipleFareServiceFactory.class);

    private List<FareServiceFactory> subFactories;

    @Override
    public FareService makeFareService() {
        List<FareService> subServices = new ArrayList<>();
        for (FareServiceFactory subFactory : subFactories)
            subServices.add(subFactory.makeFareService());
        return makeMultipleFareService(subServices);
    }

    protected abstract FareService makeMultipleFareService(List<FareService> subServices);

    @Override
    public void processGtfs(GtfsRelationalDao dao) {
        for (FareServiceFactory subFactory : subFactories)
            subFactory.processGtfs(dao);
    }

    /**
     * Accept several ways to define fares to compose. Examples:
     * 
     * <pre>
     * { combinationStrategy : "additive",
     *   // An array of 'fares'
     *   fares : [ "seattle", { ... } ]
     * }
     * --------------------------
     * { combinationStrategy : "additive",
     *   // All properties starting with 'fare'
     *   fare1 : "seattle",
     *   fare2 : { type: "bike-rental-time-based",
     *             prices : { ... }
     * } }
     * </pre>
     */
    @Override
    public void configure(JsonNode config) {
        subFactories = new ArrayList<>();
        for (JsonNode pConfig : config.path("fares")) {
            subFactories.add(DefaultFareServiceFactory.fromConfig(pConfig));
        }
        for (Iterator<Map.Entry<String, JsonNode>> i = config.fields(); i.hasNext();) {
            Map.Entry<String, JsonNode> kv = i.next();
            String key = kv.getKey();
            if (key.startsWith("fare") && !key.equals("fares")) {
                JsonNode node = kv.getValue();
                FareServiceFactory fareFactory = DefaultFareServiceFactory.fromConfig(node);
                if (fareFactory != null)
                    subFactories.add(fareFactory);
            }
        }
        if (subFactories.isEmpty())
            throw new IllegalArgumentException(
                    "Empty fare composite. Please specify either a 'fares' array or a list of 'fareXxx' properties");
        if (subFactories.size() == 1) {
            // Legal, but suspicious.
            log.warn("Fare composite has only ONE fare to combine. This is allowed, but useless. Did you forgot to define a second fare to combine?");
        }
    }

    public static class AddingMultipleFareServiceFactory extends MultipleFareServiceFactory {
        @Override
        protected FareService makeMultipleFareService(List<FareService> subServices) {
            return new AddingMultipleFareService(subServices);
        }
    }
}
