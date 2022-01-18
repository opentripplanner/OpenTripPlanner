package org.opentripplanner.graph_builder.module.vehicle;

import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingHelper;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.updater.DataSource;
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

    private DataSource<VehicleParking> dataSource;

    public void setDataSource(DataSource<VehicleParking> dataSource) {
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
        Collection<VehicleParking> vehicleParks = dataSource.getUpdates();

        for (VehicleParking vehicleParking : vehicleParks) {
            service.addVehicleParking(vehicleParking);
            VehicleParkingHelper.linkVehicleParkingToGraph(graph, vehicleParking);
        }
        LOG.info("Created " + vehicleParks.size() + " vehicle parks.");
    }

    @Override
    public void checkInputs() {
    }
}
