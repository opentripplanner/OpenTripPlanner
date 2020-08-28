package org.opentripplanner.updater.vehicle_sharing.parking_zones;

import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.CoordinateXY;
import org.opentripplanner.routing.core.vehicle_sharing.CarDescription;
import org.opentripplanner.routing.core.vehicle_sharing.FuelType;
import org.opentripplanner.routing.core.vehicle_sharing.Gearbox;
import org.opentripplanner.routing.core.vehicle_sharing.Provider;
import org.opentripplanner.routing.edgetype.rentedgetype.DropoffVehicleEdge;
import org.opentripplanner.routing.edgetype.rentedgetype.ParkingZoneInfo;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TemporaryRentVehicleVertex;

import java.util.Map;
import java.util.Optional;

import static java.util.Collections.*;
import static org.mockito.Mockito.*;

public class ParkingZonesGraphWriterRunnableTest {

    private static final CarDescription CAR = new CarDescription("1", 0, 0, FuelType.ELECTRIC, Gearbox.AUTOMATIC, new Provider(1, "PANEK"));

    private static final ParkingZoneInfo parkingZones = new ParkingZoneInfo(emptyList(), emptyList());

    private Graph graph;
    private ParkingZonesCalculator calculator;

    @Before
    public void setUp() {
        graph = new Graph();
        calculator = mock(ParkingZonesCalculator.class);
    }

    @Test
    public void shouldAddCalculatorForGraphAndUpdateParkingZonesForRentingEdges() {
        // given
        DropoffVehicleEdge edge = mock(DropoffVehicleEdge.class);
        Map<DropoffVehicleEdge, ParkingZoneInfo> parkingZonesPerVertex = singletonMap(edge, parkingZones);

        ParkingZonesGraphWriterRunnable runnable = new ParkingZonesGraphWriterRunnable(calculator, parkingZonesPerVertex);

        // when
        runnable.run(graph);

        // then
        graph.parkingZonesCalculator = calculator;
        verify(edge, times(1)).setParkingZones(parkingZones);
        verifyNoMoreInteractions(edge);
        verifyZeroInteractions(calculator);
    }

    @Test
    public void shouldUpdateParkingZonesForExistingVehicles() {
        // given
        RentVehicleEdge edge = mock(RentVehicleEdge.class);
        TemporaryRentVehicleVertex vertex = new TemporaryRentVehicleVertex("id", new CoordinateXY(1, 2), "name");
        vertex.addIncoming(edge);
        vertex.addOutgoing(edge);
        graph.vehiclesTriedToLink.put(CAR, Optional.of(vertex));
        when(calculator.getParkingZonesForEdge(edge)).thenReturn(parkingZones);

        ParkingZonesGraphWriterRunnable runnable = new ParkingZonesGraphWriterRunnable(calculator, emptyMap());

        // when
        runnable.run(graph);

        // then
        verify(calculator, times(1)).getParkingZonesForEdge(edge);
        verify(edge, times(1)).setParkingZones(parkingZones);
        verifyNoMoreInteractions(calculator, edge);
    }
}
