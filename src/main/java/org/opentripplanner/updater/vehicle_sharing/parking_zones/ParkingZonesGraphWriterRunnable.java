package org.opentripplanner.updater.vehicle_sharing.parking_zones;

import org.opentripplanner.routing.edgetype.rentedgetype.DropoffVehicleEdge;
import org.opentripplanner.routing.edgetype.rentedgetype.EdgeWithParkingZones;
import org.opentripplanner.routing.edgetype.rentedgetype.ParkingZoneInfo.SingleParkingZone;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.updater.GraphWriterRunnable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class ParkingZonesGraphWriterRunnable implements GraphWriterRunnable {

    private final ParkingZonesCalculator calculator;
    private final Map<DropoffVehicleEdge, List<SingleParkingZone>> parkingZonesPerVertex;
    private final List<SingleParkingZone> parkingZonesEnabled;

    public ParkingZonesGraphWriterRunnable(ParkingZonesCalculator calculator,
                                           Map<DropoffVehicleEdge, List<SingleParkingZone>> parkingZonesPerVertex,
                                           List<SingleParkingZone> parkingZonesEnabled) {
        this.calculator = calculator;
        this.parkingZonesPerVertex = parkingZonesPerVertex;
        this.parkingZonesEnabled = parkingZonesEnabled;
    }

    @Override
    public void run(Graph graph) {
        graph.parkingZonesCalculator = calculator;
        parkingZonesPerVertex.forEach((e, parkingZones) -> e.updateParkingZones(parkingZonesEnabled, parkingZones));
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
                .forEach(edge -> {
                    List<SingleParkingZone> parkingZones = calculator.getParkingZonesForEdge(edge, parkingZonesEnabled);
                    edge.updateParkingZones(parkingZonesEnabled, parkingZones);
                });
    }
}
