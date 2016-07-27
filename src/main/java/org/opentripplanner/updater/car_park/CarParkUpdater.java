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

package org.opentripplanner.updater.car_park;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.opentripplanner.graph_builder.linking.SimpleStreetSplitter;
import org.opentripplanner.routing.car_park.CarPark;
import org.opentripplanner.routing.car_park.CarParkService;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.ParkAndRideEdge;
import org.opentripplanner.routing.edgetype.ParkAndRideLinkEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.vertextype.ParkAndRideVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.updater.JsonConfigurable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamic car park updater which encapsulate one CarParkDataSource.
 *
 * Usage example ('fietsstalling' name is an example) in the file 'Graph.properties':
 *
 * <pre>
 * fietsstalling.type = car-park
 * fietsstalling.frequencySec = 600
 * fietsstalling.sourceType = hsl-parkandride
 * fietsstalling.url = http://host.tld/fietsstalling.kml
 * </pre>
 *
 * @author laurent
 * @author GoAbout
 * @author hannesj
 */
public class CarParkUpdater extends PollingGraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(CarParkUpdater.class);

    private GraphUpdaterManager updaterManager;

    Map<CarPark, ParkAndRideVertex> verticesByPark = new HashMap<>();

    private CarParkDataSource source;

    private Graph graph;

    private CarParkService carParkService;

    private StreetVertexIndexService streetIndex;

    private GeometryFactory gf = new GeometryFactory();

    private SimpleStreetSplitter linker;

    public CarParkUpdater() {
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    protected void configurePolling(Graph graph, JsonNode config) throws Exception {
        // Set source from preferences
        String sourceType = config.path("sourceType").asText();
        CarParkDataSource source = null;
        if (sourceType != null) {
            if (sourceType.equals("hsl-parkandride")) {
                source = new HslCarParkDataSource();
            }
        }

        if (source == null) {
            throw new IllegalArgumentException("Unknown car park source type: " + sourceType);
        } else if (source instanceof JsonConfigurable) {
            ((JsonConfigurable) source).configure(graph, config);
        }

        // Configure updater
        this.graph = graph;
        this.source = source;
        LOG.info("Creating car-park updater running every {} seconds : {}", frequencySec, source);
    }

    @Override
    public void setup() throws InterruptedException, ExecutionException {
        // Creation of network linker library will not modify the graph
        linker = new SimpleStreetSplitter(graph);

        streetIndex = graph.streetIndex;

        // Adding a car parking service needs a graph writer runnable
        updaterManager.executeBlocking(new GraphWriterRunnable() {
            @Override
            public void run(Graph graph) {
                carParkService = graph.getService(CarParkService.class, true);
            }
        });
    }

    @Override
    protected void runPolling() throws Exception {
        LOG.debug("Updating car parks from " + source);
        if (!source.update()) {
            LOG.debug("No updates");
            return;
        }
        List<CarPark> carParks = source.getCarParks();

        // Create graph writer runnable to apply these stations to the graph
        CarParkGraphWriterRunnable graphWriterRunnable = new CarParkGraphWriterRunnable(carParks);
        updaterManager.execute(graphWriterRunnable);
    }

    @Override
    public void teardown() {
    }

    private class CarParkGraphWriterRunnable implements GraphWriterRunnable {

        private List<CarPark> carParks;

        private CarParkGraphWriterRunnable(List<CarPark> carParks) {
            this.carParks = carParks;
        }

        @Override
        public void run(Graph graph) {
            // Apply stations to graph
            Set<CarPark> carParkSet = new HashSet<CarPark>();
            /* Add any new park and update space available for existing parks */
            for (CarPark carPark : carParks) {
                carParkService.addCarPark(carPark);
                carParkSet.add(carPark);
                if (verticesByPark.get(carPark) == null) {
                    ParkAndRideVertex carParkVertex = new ParkAndRideVertex(graph, carPark);
                    new ParkAndRideEdge(carParkVertex);
                    Envelope envelope = carPark.geometry.getEnvelopeInternal();
                    long numberOfVertices = streetIndex
                        .getVerticesForEnvelope(envelope)
                        .stream()
                        .filter(vertex -> vertex instanceof StreetVertex)
                        .filter(vertex -> gf.createPoint(vertex.getCoordinate()).within(carPark.geometry))
                        .peek(vertex -> new ParkAndRideLinkEdge(vertex, carParkVertex))
                        .peek(vertex -> new ParkAndRideLinkEdge(carParkVertex, vertex))
                        .count();
                    if (numberOfVertices == 0) {
                        if (!(linker.link(carParkVertex, TraverseMode.CAR, null) &&
                              linker.link(carParkVertex, TraverseMode.WALK, null))) {
                            LOG.warn("{} not near any streets; it will not be usable.", carPark);
                        }
                    }
                    verticesByPark.put(carPark, carParkVertex);
                } else {
                    verticesByPark.get(carPark).spacesAvailable = carPark.spacesAvailable;
                }
            }
            /* Remove existing parks that were not present in the update */
            List<CarPark> toRemove = new ArrayList<CarPark>();
            for (Map.Entry<CarPark, ParkAndRideVertex> entry : verticesByPark.entrySet()) {
                CarPark carPark = entry.getKey();
                if (carParkSet.contains(carPark))
                    continue;
                ParkAndRideVertex vertex = entry.getValue();
                if (graph.containsVertex(vertex)) {
                    graph.removeVertexAndEdges(vertex);
                }
                toRemove.add(carPark);
                carParkService.removeCarPark(carPark);
                // TODO: need to unsplit any streets that were split
            }
            for (CarPark carPark : toRemove) {
                // post-iteration removal to avoid concurrent modification
                verticesByPark.remove(carPark);
            }
        }
    }
}
