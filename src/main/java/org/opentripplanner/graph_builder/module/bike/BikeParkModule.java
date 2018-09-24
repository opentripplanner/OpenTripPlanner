package org.opentripplanner.graph_builder.module.bike;

import java.util.Collection;
import java.util.HashMap;

import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.edgetype.BikeParkEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.BikeParkVertex;
import org.opentripplanner.updater.bike_park.BikeParkDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This graph builder allow one to statically build bike park using the same source as the dynamic
 * bike park updater.
 * 
 * Bike park-and-ride and "OV-fiets mode" development has been funded by GoAbout
 * (https://goabout.com/).
 * 
 * @author laurent
 * @author GoAbout
 */
public class BikeParkModule implements GraphBuilderModule {

    private static Logger LOG = LoggerFactory.getLogger(BikeParkModule.class);

    private BikeParkDataSource dataSource;

    public void setDataSource(BikeParkDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {

        LOG.info("Building bike parks from static source...");
        BikeRentalStationService service = graph.getService(BikeRentalStationService.class, true);
        if (!dataSource.update()) {
            LOG.warn("No bike parks found from the data source.");
            return;
        }
        Collection<BikePark> bikeParks = dataSource.getBikeParks();

        for (BikePark bikePark : bikeParks) {
            service.addBikePark(bikePark);
            BikeParkVertex bikeParkVertex = new BikeParkVertex(graph, bikePark);
            new BikeParkEdge(bikeParkVertex);
        }
        LOG.info("Created " + bikeParks.size() + " bike parks.");
    }

    @Override
    public void checkInputs() {
    }
}
