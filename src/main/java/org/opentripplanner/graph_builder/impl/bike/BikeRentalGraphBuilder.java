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

package org.opentripplanner.graph_builder.impl.bike;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.opentripplanner.graph_builder.annotation.BikeRentalStationUnlinked;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.edgetype.RentABikeOffEdge;
import org.opentripplanner.routing.edgetype.RentABikeOnEdge;
import org.opentripplanner.routing.edgetype.loader.LinkRequest;
import org.opentripplanner.routing.edgetype.loader.NetworkLinkerLibrary;
import org.opentripplanner.routing.graph.Edge;
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
public class BikeRentalGraphBuilder implements GraphBuilder {

    private static Logger LOG = LoggerFactory.getLogger(BikeRentalGraphBuilder.class);

    private BikeRentalDataSource dataSource;

    public void setDataSource(BikeRentalDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {

        LOG.info("Building bike rental stations from static source...");
        NetworkLinkerLibrary networkLinkerLibrary = new NetworkLinkerLibrary(graph, extra);
        BikeRentalStationService service = graph.getService(BikeRentalStationService.class, true);
        if (!dataSource.update()) {
            LOG.warn("No bike rental found from the data source.");
            return;
        }
        Collection<BikeRentalStation> stations = dataSource.getStations();

        for (BikeRentalStation station : stations) {
            service.addBikeRentalStation(station);
            BikeRentalStationVertex vertex = new BikeRentalStationVertex(graph, station);
            if (!networkLinkerLibrary.connectVertexToStreets(vertex).getResult()) {
                LOG.warn(graph.addBuilderAnnotation(new BikeRentalStationUnlinked(vertex)));
            }
            LinkRequest request = networkLinkerLibrary.connectVertexToStreets(vertex);
            for (Edge e : request.getEdgesAdded()) {
                graph.addTemporaryEdge(e);
            }
            new RentABikeOnEdge(vertex, vertex, station.networks);
            new RentABikeOffEdge(vertex, vertex, station.networks);
        }
        LOG.info("Created " + stations.size() + " bike rental stations.");
    }

    @Override
    public List<String> provides() {
        return Arrays.asList("bike_rental");
    }

    @Override
    public List<String> getPrerequisites() {
        return Arrays.asList("streets");
    }

    @Override
    public void checkInputs() {
    }
}
