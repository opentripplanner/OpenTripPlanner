package org.opentripplanner.updater.vehicle_sharing.parking_zones;

import org.opentripplanner.routing.edgetype.rentedgetype.ParkingZoneInfo.SingleParkingZone;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleAnywhereEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphWriterRunnable;

import java.util.List;
import java.util.Map;

class ParkingZonesGraphWriterRunnable implements GraphWriterRunnable {

    private final ParkingZonesCalculator calculator;
    private final Map<RentVehicleAnywhereEdge, List<SingleParkingZone>> parkingZonesPerVertex;
    private final List<SingleParkingZone> parkingZonesEnabled;

    public ParkingZonesGraphWriterRunnable(ParkingZonesCalculator calculator,
                                           Map<RentVehicleAnywhereEdge, List<SingleParkingZone>> parkingZonesPerVertex,
                                           List<SingleParkingZone> parkingZonesEnabled) {
        this.calculator = calculator;
        this.parkingZonesPerVertex = parkingZonesPerVertex;
        this.parkingZonesEnabled = parkingZonesEnabled;
    }

    @Override
    public void run(Graph graph) {
        graph.parkingZonesCalculator = calculator;
        parkingZonesPerVertex.forEach((e, parkingZones) -> e.updateParkingZones(parkingZonesEnabled, parkingZones));
    }
}
