package org.opentripplanner.updater.vehicle_sharing;

import org.apache.commons.lang3.tuple.Pair;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleAnywhereEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphWriterRunnable;

import java.util.List;
import java.util.Map;

class VehicleSharingGraphWriterRunnable implements GraphWriterRunnable {

    List<Pair<Edge,VehicleDescription>> appeared;
    List<RentVehicleAnywhereEdge> rememberedVehicles;

    public VehicleSharingGraphWriterRunnable(List<Pair<Edge,VehicleDescription>> appeared, List<RentVehicleAnywhereEdge> rememberedVehicles) {
        this.appeared = appeared;
        this.rememberedVehicles = rememberedVehicles;
    }

    @Override
    public void run(Graph graph) {
        for(RentVehicleAnywhereEdge edge : rememberedVehicles){
            edge.avaiableVehicles.clear();
        }
        for (Pair<Edge,VehicleDescription> p : appeared) {
            ((RentVehicleAnywhereEdge) p.getLeft()).avaiableVehicles.add(p.getRight());
        }
    }
}
