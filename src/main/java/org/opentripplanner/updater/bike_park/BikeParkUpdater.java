package org.opentripplanner.updater.bike_park;

import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.linking.SimpleStreetSplitter;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.edgetype.BikeParkEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.BikeParkVertex;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Graph updater that dynamically sets availability information on bike parking lots.
 * This updater fetches data from a single BikeParkDataSource.
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

    private final Map<BikePark, BikeParkVertex> verticesByPark = new HashMap<>();

    private final BikeParkDataSource source;

    private SimpleStreetSplitter linker;

    private BikeRentalStationService bikeService;

    public BikeParkUpdater(BikeParkUpdaterParameters parameters) {
        super(parameters);
        // Set source from preferences
        source = new KmlBikeParkDataSource(parameters.sourceParameters());

        LOG.info("Creating bike-park updater running every {} seconds : {}", pollingPeriodSeconds, source);
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    public void setup(Graph graph) {
        // Creation of network linker library will not modify the graph
        linker = new SimpleStreetSplitter(graph, new DataImportIssueStore(false));
        // Adding a bike park station service needs a graph writer runnable
        bikeService = graph.getService(BikeRentalStationService.class, true);
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

        private final List<BikePark> bikeParks;

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
                        LOG.info("Bike park {} unlinked", bikeParkVertex);
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
