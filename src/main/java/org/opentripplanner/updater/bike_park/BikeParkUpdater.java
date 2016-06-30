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

package org.opentripplanner.updater.bike_park;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.graph_builder.linking.SimpleStreetSplitter;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.edgetype.BikeParkEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.BikeParkVertex;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.updater.JsonConfigurable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamic bike park updater which encapsulate one BikeParkDataSource.
 * 
 * Usage example ('fietsstalling' name is an example) in the file 'Graph.properties':
 * 
 * <pre>
 * fietsstalling.type = bike-park
 * fietsstalling.frequencySec = 600
 * fietsstalling.sourceType = kml-placemarks
 * fietsstalling.url = http://host.tld/fietsstalling.kml
 * </pre>
 * 
 * Bike park-and-ride and "OV-fiets mode" development has been funded by GoAbout
 * (https://goabout.com/).
 * 
 * @author laurent
 * @author GoAbout
 */
public class BikeParkUpdater extends PollingGraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(BikeParkUpdater.class);

    private GraphUpdaterManager updaterManager;

    Map<BikePark, BikeParkVertex> verticesByPark = new HashMap<BikePark, BikeParkVertex>();

    private BikeParkDataSource source;

    private Graph graph;

    private SimpleStreetSplitter linker;

    private BikeRentalStationService bikeService;

    public BikeParkUpdater() {
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    protected void configurePolling(Graph graph, JsonNode config) throws Exception {
        // Set source from preferences
        String sourceType = config.path("sourceType").asText();
        BikeParkDataSource source = null;
        if (sourceType != null) {
            if (sourceType.equals("kml-placemarks")) {
                source = new KmlBikeParkDataSource();
            }
        }

        if (source == null) {
            throw new IllegalArgumentException("Unknown bike rental source type: " + sourceType);
        } else if (source instanceof JsonConfigurable) {
            ((JsonConfigurable) source).configure(graph, config);
        }

        // Configure updater
        this.graph = graph;
        this.source = source;
        LOG.info("Creating bike-park updater running every {} seconds : {}", frequencySec, source);
    }

    @Override
    public void setup() throws InterruptedException, ExecutionException {
        // Creation of network linker library will not modify the graph
        linker = new SimpleStreetSplitter(graph);

        // Adding a bike park station service needs a graph writer runnable
        updaterManager.executeBlocking(new GraphWriterRunnable() {
            @Override
            public void run(Graph graph) {
                bikeService = graph.getService(BikeRentalStationService.class, true);
            }
        });
    }

    @Override
    protected void runPolling() throws Exception {
        LOG.debug("Updating bike parks from " + source);
        if (!source.update()) {
            LOG.debug("No updates");
            return;
        }
        List<BikePark> bikeParks = source.getBikeParks();

        // Create graph writer runnable to apply these stations to the graph
        BikeParkGraphWriterRunnable graphWriterRunnable = new BikeParkGraphWriterRunnable(bikeParks);
        updaterManager.execute(graphWriterRunnable);
    }

    @Override
    public void teardown() {
    }

    private class BikeParkGraphWriterRunnable implements GraphWriterRunnable {

        private List<BikePark> bikeParks;

        private BikeParkGraphWriterRunnable(List<BikePark> bikeParks) {
            this.bikeParks = bikeParks;
        }

        @Override
        public void run(Graph graph) {
            // Apply stations to graph
            Set<BikePark> bikeParkSet = new HashSet<BikePark>();
            /* Add any new park and update space available for existing parks */
            for (BikePark bikePark : bikeParks) {
                bikeService.addBikePark(bikePark);
                bikeParkSet.add(bikePark);
                BikeParkVertex bikeParkVertex = verticesByPark.get(bikePark);
                if (bikeParkVertex == null) {
                    bikeParkVertex = new BikeParkVertex(graph, bikePark);
                    if (!linker.link(bikeParkVertex)) {
                        // the toString includes the text "Bike park"
                        LOG.warn("{} not near any streets; it will not be usable.", bikePark);
                    }
                    verticesByPark.put(bikePark, bikeParkVertex);
                    new BikeParkEdge(bikeParkVertex);
                } else {
                    bikeParkVertex.setSpacesAvailable(bikePark.spacesAvailable);
                }
            }
            /* Remove existing parks that were not present in the update */
            List<BikePark> toRemove = new ArrayList<BikePark>();
            for (Entry<BikePark, BikeParkVertex> entry : verticesByPark.entrySet()) {
                BikePark bikePark = entry.getKey();
                if (bikeParkSet.contains(bikePark))
                    continue;
                BikeParkVertex vertex = entry.getValue();
                if (graph.containsVertex(vertex)) {
                    graph.removeVertexAndEdges(vertex);
                }
                toRemove.add(bikePark);
                bikeService.removeBikePark(bikePark);
                // TODO: need to unsplit any streets that were split
            }
            for (BikePark bikePark : toRemove) {
                // post-iteration removal to avoid concurrent modification
                verticesByPark.remove(bikePark);
            }
        }
    }
}
