package org.opentripplanner.updater.vehicle_sharing.vehicles_positions;

import org.apache.commons.lang3.tuple.Pair;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleAnywhereEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphWriterRunnable;

import java.util.List;

class VehicleSharingGraphWriterRunnable implements GraphWriterRunnable {

    private final List<Pair<RentVehicleAnywhereEdge, VehicleDescription>> appeared;
    private final List<RentVehicleAnywhereEdge> rememberedVehicles;

    public VehicleSharingGraphWriterRunnable(List<Pair<RentVehicleAnywhereEdge, VehicleDescription>> appeared,
                                             List<RentVehicleAnywhereEdge> rememberedVehicles) {
        this.appeared = appeared;
        this.rememberedVehicles = rememberedVehicles;
    }

    @Override
    public void run(Graph graph) {
        for (RentVehicleAnywhereEdge edge : rememberedVehicles) {
            // TODO remove only vehicles which disappeared
            edge.getAvailableVehicles().clear();
        }
        for (Pair<RentVehicleAnywhereEdge, VehicleDescription> p : appeared) {
            p.getLeft().getAvailableVehicles().add(p.getRight());
        }
    }
}
