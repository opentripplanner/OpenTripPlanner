package org.opentripplanner.updater.vehicle_sharing.parking_zones;

import org.apache.commons.lang3.tuple.Pair;
import org.opentripplanner.routing.edgetype.rentedgetype.ParkingZoneInfo.SingleParkingZone;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleAnywhereEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphWriterRunnable;

import java.util.List;

class ParkingZonesGraphWriterRunnable implements GraphWriterRunnable {

    private final List<Pair<RentVehicleAnywhereEdge, List<SingleParkingZone>>> parkingZonesPerVertex;
    private final List<SingleParkingZone> parkingZonesEnabled;

    public ParkingZonesGraphWriterRunnable(List<Pair<RentVehicleAnywhereEdge, List<SingleParkingZone>>> parkingZonesPerVertex,
                                           List<SingleParkingZone> parkingZonesEnabled) {
        this.parkingZonesPerVertex = parkingZonesPerVertex;
        this.parkingZonesEnabled = parkingZonesEnabled;
    }

    @Override
    public void run(Graph graph) {
        graph.parkingZonesEnabled.updateParkingZones(parkingZonesEnabled);
        for (Pair<RentVehicleAnywhereEdge, List<SingleParkingZone>> p : parkingZonesPerVertex) {
            p.getLeft().getParkingZones().updateParkingZones(p.getRight());
        }
    }
}
