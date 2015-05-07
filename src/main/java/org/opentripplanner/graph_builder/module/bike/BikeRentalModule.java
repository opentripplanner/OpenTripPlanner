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

package org.opentripplanner.graph_builder.module.bike;

import java.util.Collection;
import java.util.HashMap;

import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.edgetype.RentABikeOffEdge;
import org.opentripplanner.routing.edgetype.RentABikeOnEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.updater.bike_rental.BikeRentalDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This graph builder allow one to statically build bike rental stations using the same source as
 * the dynamic bike rental updater. This may help when the source does not contain real-time info
 * (or one is not interested in), location of stations do not change that often, or development.
 */
public class BikeRentalModule implements GraphBuilderModule {

    private static Logger LOG = LoggerFactory.getLogger(BikeRentalModule.class);

    private BikeRentalDataSource dataSource;

    public void setDataSource(BikeRentalDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {

        LOG.info("Building bike rental stations from static source...");
        BikeRentalStationService service = graph.getService(BikeRentalStationService.class, true);
        if (!dataSource.update()) {
            LOG.warn("No bike rental found from the data source.");
            return;
        }
        Collection<BikeRentalStation> stations = dataSource.getStations();

        for (BikeRentalStation station : stations) {
            service.addBikeRentalStation(station);
            BikeRentalStationVertex vertex = new BikeRentalStationVertex(graph, station);
            new RentABikeOnEdge(vertex, vertex, station.networks);
            if (station.allowDropoff)
                new RentABikeOffEdge(vertex, vertex, station.networks);
        }
        LOG.info("Created " + stations.size() + " bike rental stations.");
    }

    @Override
    public void checkInputs() {
    }
}
