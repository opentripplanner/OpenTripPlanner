package org.opentripplanner.updater.vehicle_sharing.parking_zones;

import org.junit.Test;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleType;
import org.opentripplanner.routing.edgetype.rentedgetype.ParkingZoneInfo.SingleParkingZone;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleAnywhereEdge;
import org.opentripplanner.routing.graph.Graph;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.*;

public class ParkingZonesGraphWriterRunnableTest {

    @Test
    public void shouldAddCalculatorForGraphAndUpdateParkingZonesForRentingEdges() {
        // given
        Graph graph = new Graph();
        ParkingZonesCalculator calculator = new ParkingZonesCalculator(null);
        List<SingleParkingZone> parkingZonesEnabled = singletonList(new SingleParkingZone(1, VehicleType.CAR));
        List<SingleParkingZone> parkingZonesForEdge = singletonList(new SingleParkingZone(1, VehicleType.CAR));
        RentVehicleAnywhereEdge edge = mock(RentVehicleAnywhereEdge.class);
        Map<RentVehicleAnywhereEdge, List<SingleParkingZone>> parkingZonesPerVertex = singletonMap(edge, parkingZonesForEdge);

        ParkingZonesGraphWriterRunnable runnable = new ParkingZonesGraphWriterRunnable(calculator, parkingZonesPerVertex, parkingZonesEnabled);

        // when
        runnable.run(graph);

        // then
        graph.parkingZonesCalculator = calculator;
        verify(edge, times(1)).updateParkingZones(parkingZonesEnabled, parkingZonesForEdge);
    }

}
