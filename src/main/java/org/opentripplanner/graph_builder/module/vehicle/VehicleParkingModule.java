package org.opentripplanner.graph_builder.module.vehicle;

import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.edgetype.VehicleParkingEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.routing.vertextype.VehicleParkingVertex;
import org.opentripplanner.updater.bike_park.BikeParkDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;

/**
 * This graph builder allow one to statically build bike or car park using the same source as the dynamic
 * vehicle parking updater.
 */
public class VehicleParkingModule implements GraphBuilderModule {

    private final static Logger LOG = LoggerFactory.getLogger(VehicleParkingModule.class);

    private BikeParkDataSource dataSource;

    public void setDataSource(BikeParkDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void buildGraph(
            Graph graph,
            HashMap<Class<?>, Object> extra,
            DataImportIssueStore issueStore
    ) {

        LOG.info("Building vehicle parking from static source...");
        VehicleParkingService service = graph.getService(VehicleParkingService.class, true);
        if (!dataSource.update()) {
            LOG.warn("No vehicle parks found from the data source.");
            return;
        }
        Collection<VehicleParking> vehicleParks = dataSource.getBikeParks();

        for (VehicleParking vehicleParking : vehicleParks) {
            service.addVehicleParking(vehicleParking);
            VehicleParkingVertex vehicleParkingVertex = new VehicleParkingVertex(graph, vehicleParking);
            new VehicleParkingEdge(vehicleParkingVertex);
        }
        LOG.info("Created " + vehicleParks.size() + " vehicle parks.");
    }

    @Override
    public void checkInputs() {
    }
}
