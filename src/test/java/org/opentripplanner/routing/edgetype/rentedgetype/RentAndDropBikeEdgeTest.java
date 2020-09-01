package org.opentripplanner.routing.edgetype.rentedgetype;

import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.CoordinateXY;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.*;
import org.opentripplanner.routing.core.vehicle_sharing.*;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TemporaryRentVehicleVertex;
import org.opentripplanner.updater.vehicle_sharing.parking_zones.ParkingZonesCalculator;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RentAndDropBikeEdgeTest {
    private static final CarDescription CAR_1 = new CarDescription("1", 0, 0, FuelType.ELECTRIC, Gearbox.AUTOMATIC, new Provider(1, "PANEK"));

    private static final BikeRentalStation station11 = new BikeRentalStation("11", 0, 0, 1, 1, new Provider(1, "provider1"));
    private static final BikeRentalStation station12 = new BikeRentalStation("12", 1, 1, 1, 1, new Provider(1, "provider1"));

    private static final BikeRentalStation station21 = new BikeRentalStation("21", 0, 0, 1, 1, new Provider(1, "provider2"));
    TemporaryRentVehicleVertex v1 = new TemporaryRentVehicleVertex("v1", new CoordinateXY(0, 0), "name");

    private RoutingRequest request;
    private State state, rentingState;
    private BikeDescription bike1;

    private RentVehicleEdge rentEdge11;
    private DropoffVehicleEdge dropEdge11;
    private DropoffVehicleEdge dropEdge21;
    private Graph graph = new Graph();

    @Before
    public void setUp() {

        bike1 = station11.getBikeFromStation();


        request = new RoutingRequest();
        request.setDummyRoutingContext(graph);
        request.setModes(new TraverseModeSet(TraverseMode.WALK, TraverseMode.BICYCLE, TraverseMode.CAR));
        request.setStartingMode(TraverseMode.WALK);

        request.vehicleValidator = mock(VehicleValidator.class);
        request.rentingAllowed = true;
        state = new State(v1, request);

        rentEdge11 = new RentVehicleEdge(v1, bike1);
        rentEdge11.setBikeRentalStation(station11);

        dropEdge11 = new DropoffVehicleEdge(v1);
        dropEdge21 = new DropoffVehicleEdge(v1);

        ParkingZoneInfo parkingZones = mock(ParkingZoneInfo.class);

        rentEdge11.setParkingZones(parkingZones);
        StateEditor se = state.edit(rentEdge11);
        se.beginVehicleRenting(bike1);
        rentingState = se.makeState();

        graph.parkingZonesCalculator = new ParkingZonesCalculator(Collections.emptyList());
        graph.parkingZonesCalculator.enableNewParkingZone(new BikeStationParkingZone(station11), graph);
        graph.parkingZonesCalculator.enableNewParkingZone(new BikeStationParkingZone(station21), graph);
    }

    @Test
    public void shouldDropBikeInMatchingProviderStation() {
        //when
        dropEdge11.setParkingZones(new ParkingZoneInfo(Collections.singletonList(new BikeStationParkingZone(station11)), graph.parkingZonesCalculator.getParkingZonesEnabled()));

        //given
        State traversed = dropEdge11.traverse(rentingState);

        //then
        assertNotNull(traversed);
    }

    @Test
    public void shouldNotDropBikeInDifferentProviderStation() {
        //when
        dropEdge21.setParkingZones(new ParkingZoneInfo(Collections.singletonList(new BikeStationParkingZone(station21)), graph.parkingZonesCalculator.getParkingZonesEnabled()));

        //given
        State traversed = dropEdge11.traverse(rentingState);

        //then
        assertNull(traversed);
    }

    @Test
    public void takeAvaiableBike() {
        //when
        station11.bikesAvailable = 1;
        when(request.vehicleValidator.isValid(bike1)).thenReturn(true);

        //given
        State rented = rentEdge11.traverse(state);

        //then
        assertNotNull(rented);
    }

    @Test
    public void dontTakeBikeFromEmptyStation() {
        //when
        station11.bikesAvailable = 0;
        when(request.vehicleValidator.isValid(bike1)).thenReturn(true);

        //given
        State rented = rentEdge11.traverse(state);

        //then
        assertNull(rented);
    }

    @Test
    public void dontLeaveBikeOnFullStation() {
        //when
        dropEdge11.setParkingZones(new ParkingZoneInfo(Collections.singletonList(new BikeStationParkingZone(station12)), graph.parkingZonesCalculator.getParkingZonesEnabled()));
        station12.spacesAvailable = 0;

        //given
        State traversed = dropEdge11.traverse(rentingState);

        //then
        assertNull(traversed);
    }

    @Test
    public void changeCarForBike() {
        //when
        RentVehicleEdge rentCarEdge = new RentVehicleEdge(v1, CAR_1);
        when(request.vehicleValidator.isValid(CAR_1)).thenReturn(true);
        when(request.vehicleValidator.isValid(bike1)).thenReturn(true);
        when(rentEdge11.canDropoffVehicleHere(CAR_1)).thenReturn(true);


        State carState = rentCarEdge.traverse(state);

//        given
        State bikeState = rentEdge11.traverse(carState);

        //then
        assertNotNull(carState);
        assertNotNull(bikeState);
        assertEquals(bikeState.getCurrentVehicleType(), VehicleType.BIKE);
    }


}
