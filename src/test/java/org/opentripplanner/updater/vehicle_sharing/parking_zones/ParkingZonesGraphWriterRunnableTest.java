package org.opentripplanner.updater.vehicle_sharing.parking_zones;

import org.junit.Test;
import org.locationtech.jts.geom.CoordinateXY;
import org.opentripplanner.routing.core.vehicle_sharing.*;
import org.opentripplanner.routing.edgetype.rentedgetype.DropoffVehicleEdge;
import org.opentripplanner.routing.edgetype.rentedgetype.ParkingZoneInfo.SingleParkingZone;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TemporaryRentVehicleVertex;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.*;
import static org.mockito.Mockito.*;

public class ParkingZonesGraphWriterRunnableTest {

    private static final CarDescription CAR = new CarDescription("1", 0, 0, FuelType.ELECTRIC, Gearbox.AUTOMATIC, new Provider(1, "PANEK"));

    private final List<SingleParkingZone> parkingZonesEnabled = singletonList(new SingleParkingZone(1, VehicleType.CAR));
    private final List<SingleParkingZone> parkingZonesForEdge = singletonList(new SingleParkingZone(1, VehicleType.CAR));

    @Test
    public void shouldAddCalculatorForGraphAndUpdateParkingZonesForRentingEdges() {
        // given
        Graph graph = new Graph();
        ParkingZonesCalculator calculator = new ParkingZonesCalculator(null);
        DropoffVehicleEdge edge = mock(DropoffVehicleEdge.class);
        Map<DropoffVehicleEdge, List<SingleParkingZone>> parkingZonesPerVertex = singletonMap(edge, parkingZonesForEdge);

        ParkingZonesGraphWriterRunnable runnable = new ParkingZonesGraphWriterRunnable(calculator, parkingZonesPerVertex, parkingZonesEnabled);

        // when
        runnable.run(graph);

        // then
        graph.parkingZonesCalculator = calculator;
        verify(edge, times(1)).updateParkingZones(parkingZonesEnabled, parkingZonesForEdge);
    }

    @Test
    public void shouldUpdateParkingZonesForExistingVehicles() {
        // given
        TemporaryRentVehicleVertex vertex = new TemporaryRentVehicleVertex("id", new CoordinateXY(1, 2), "name");
        RentVehicleEdge edge = mock(RentVehicleEdge.class);
        vertex.addIncoming(edge);
        vertex.addOutgoing(edge);

        Graph graph = new Graph();
        graph.vehiclesTriedToLink.put(CAR, Optional.of(vertex));

        ParkingZonesCalculator calculator = mock(ParkingZonesCalculator.class);
        when(calculator.getParkingZonesForEdge(edge, parkingZonesEnabled)).thenReturn(parkingZonesForEdge);

        ParkingZonesGraphWriterRunnable runnable = new ParkingZonesGraphWriterRunnable(calculator, emptyMap(), parkingZonesEnabled);

        // when
        runnable.run(graph);

        // then
        verify(edge, times(1)).updateParkingZones(parkingZonesEnabled, parkingZonesForEdge);
    }
}
