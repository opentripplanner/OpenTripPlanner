package org.opentripplanner.updater.vehicle_sharing.parking_zones;

import org.opentripplanner.routing.edgetype.rentedgetype.DropoffVehicleEdge;
import org.opentripplanner.routing.edgetype.rentedgetype.EdgeWithParkingZones;
import org.opentripplanner.routing.edgetype.rentedgetype.ParkingZoneInfo;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.updater.GraphWriterRunnable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

class ParkingZonesGraphWriterRunnable implements GraphWriterRunnable {

    private final ParkingZonesCalculator calculator;
    private final Map<DropoffVehicleEdge, ParkingZoneInfo> parkingZonesPerVertex;

    public ParkingZonesGraphWriterRunnable(ParkingZonesCalculator calculator,
                                           Map<DropoffVehicleEdge, ParkingZoneInfo> parkingZonesPerVertex) {
        this.calculator = calculator;
        this.parkingZonesPerVertex = parkingZonesPerVertex;
    }

    @Override
    public void run(Graph graph) {
        graph.parkingZonesCalculator = calculator;
        parkingZonesPerVertex.forEach(EdgeWithParkingZones::setParkingZones);
        updateParkingZonesForVehiclesInGraph(graph);
    }

    private void updateParkingZonesForVehiclesInGraph(Graph graph) {
        graph.vehiclesTriedToLink.values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Vertex::getOutgoing)
                .flatMap(Collection::stream)
                .filter(EdgeWithParkingZones.class::isInstance)
                .map(EdgeWithParkingZones.class::cast)
                .forEach(edge -> edge.setParkingZones(calculator.getParkingZonesForEdge(edge)));
    }
}
