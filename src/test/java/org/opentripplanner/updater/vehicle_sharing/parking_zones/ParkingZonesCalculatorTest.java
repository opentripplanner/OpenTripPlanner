package org.opentripplanner.updater.vehicle_sharing.parking_zones;

import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleType;
import org.opentripplanner.routing.edgetype.rentedgetype.ParkingZoneInfo.SingleParkingZone;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleAnywhereEdge;
import org.opentripplanner.routing.graph.Vertex;

import java.util.List;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ParkingZonesCalculatorTest {

    private RentVehicleAnywhereEdge edge;
    private Geometry geometryAllowed;
    private Geometry geometryDisallowed;
    private ParkingZonesCalculator calculator;

    private final GeometryParkingZone geometryParkingZone1 = new GeometryParkingZone(1, VehicleType.CAR, null, null);
    private final GeometryParkingZone geometryParkingZone2 = new GeometryParkingZone(1, VehicleType.MOTORBIKE, null, null);
    private final GeometryParkingZone geometryParkingZone3 = new GeometryParkingZone(2, VehicleType.KICKSCOOTER, null, null);

    @Before
    public void setUp() {
        Vertex vertex = mock(Vertex.class);
        when(vertex.getLat()).thenReturn(1.1);
        when(vertex.getLon()).thenReturn(2.2);
        edge = new RentVehicleAnywhereEdge(vertex);
        geometryAllowed = mock(Geometry.class);
        geometryDisallowed = mock(Geometry.class);
        GeometryParkingZone geometryParkingZone = new GeometryParkingZone(1, VehicleType.CAR, singletonList(geometryAllowed), singletonList(geometryDisallowed));
        calculator = new ParkingZonesCalculator(singletonList(geometryParkingZone));
    }

    @Test
    public void shouldFindAllProvidersAndVehicleTypesWhichHaveParkingZonesEnabled() {
        // given
        ParkingZonesCalculator calculator = new ParkingZonesCalculator(of(geometryParkingZone1, geometryParkingZone2, geometryParkingZone3));

        // when
        List<SingleParkingZone> parkingZonesEnabled = calculator.getNewParkingZonesEnabled();

        // then
        assertEquals(3, parkingZonesEnabled.size());
        assertTrue(parkingZonesEnabled.contains(new SingleParkingZone(1, VehicleType.CAR)));
        assertTrue(parkingZonesEnabled.contains(new SingleParkingZone(1, VehicleType.MOTORBIKE)));
        assertTrue(parkingZonesEnabled.contains(new SingleParkingZone(2, VehicleType.KICKSCOOTER)));
    }

    @Test
    public void shouldRemoveDuplicatesInParkingZonesEnabled() {
        // given
        ParkingZonesCalculator calculator = new ParkingZonesCalculator(of(geometryParkingZone3, geometryParkingZone3));

        // when
        List<SingleParkingZone> parkingZonesEnabled = calculator.getNewParkingZonesEnabled();

        // then
        assertEquals(1, parkingZonesEnabled.size());
        assertTrue(parkingZonesEnabled.contains(new SingleParkingZone(2, VehicleType.KICKSCOOTER)));
    }

    @Test
    public void shouldNotAllowToParkInsideGeometryDisallowed() {
        // given
        when(geometryAllowed.contains(any())).thenReturn(true);
        when(geometryDisallowed.contains(any())).thenReturn(true);

        // when
        List<SingleParkingZone> parkingZonesEnabled = calculator.getNewParkingZonesEnabled();
        List<SingleParkingZone> parkingZonesForEdge = calculator.getParkingZonesForEdge(edge, parkingZonesEnabled);

        // then
        assertEquals(0, parkingZonesForEdge.size());
    }

    @Test
    public void shouldAllowToParkInsideGeometryAllowed() {
        // given
        when(geometryAllowed.contains(any())).thenReturn(true);
        when(geometryDisallowed.contains(any())).thenReturn(false);

        // when
        List<SingleParkingZone> parkingZonesEnabled = calculator.getNewParkingZonesEnabled();
        List<SingleParkingZone> parkingZonesForEdge = calculator.getParkingZonesForEdge(edge, parkingZonesEnabled);

        // then
        assertEquals(1, parkingZonesForEdge.size());
        assertTrue(parkingZonesForEdge.contains(new SingleParkingZone(1, VehicleType.CAR)));
    }

    @Test
    public void shouldNotAllowToParkOutsideGeometryAllowed() {
        // given
        when(geometryAllowed.contains(any())).thenReturn(false);

        // when
        List<SingleParkingZone> parkingZonesEnabled = calculator.getNewParkingZonesEnabled();
        List<SingleParkingZone> parkingZonesForEdge = calculator.getParkingZonesForEdge(edge, parkingZonesEnabled);

        // then
        assertEquals(0, parkingZonesForEdge.size());
    }
}
