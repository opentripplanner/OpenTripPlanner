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
import java.util.List;

import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.routing.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.services.FareServiceFactory;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class MultipleFareServiceFactory implements FareServiceFactory {

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

    @Override
    public void configure(JsonNode config) {
        subFactories = new ArrayList<>();
        for (JsonNode pConfig : config.path("fares")) {
            subFactories.add(DefaultFareServiceFactory.fromConfig(pConfig));
        }
    }

    public static class AddingMultipleFareServiceFactory extends MultipleFareServiceFactory {
        @Override
        protected FareService makeMultipleFareService(List<FareService> subServices) {
            return new AddingMultipleFareService(subServices);
        }
    }
}
