package org.opentripplanner.updater.vehicle_sharing.vehicles_positions;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.hasura_client.hasura_objects.Vehicle;
import org.opentripplanner.hasura_client.mappers.VehiclePositionsMapper;
import org.opentripplanner.routing.core.vehicle_sharing.*;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class VehiclePositionsMapperTest {

    private VehiclePositionsMapper mapper;

    private VehicleProviderHasura provider;

    private Vehicle emptyVehicle, defaultVehicle;

    @Before
    public void setUp() {
        mapper = new VehiclePositionsMapper();

        emptyVehicle = new Vehicle();


        provider = new VehicleProviderHasura();
        provider.setId(1);
        provider.setName("NextBike");

        defaultVehicle = new Vehicle();
        defaultVehicle.setProvider(provider);
        defaultVehicle.setProviderVehicleId("1234");
        defaultVehicle.setLongitude(1.0);
        defaultVehicle.setLatitude(2.0);
        defaultVehicle.setFuelType("fossil");
        defaultVehicle.setGearbox("manual");
        defaultVehicle.setRange(15.0);

    }

    @Test
    public void shouldOmitVehicleWithoutProvider() {
        // when
        List<VehicleDescription> vehicleDescriptions = mapper.map(Collections.singletonList(emptyVehicle));

        // then
        assertTrue(vehicleDescriptions.isEmpty());
    }

    @Test
    public void shouldOmitVehicleWithoutVehicleType() {
        // given
        emptyVehicle.setProvider(provider);

        // when
        List<VehicleDescription> vehicleDescriptions = mapper.map(Collections.singletonList(emptyVehicle));

        // then
        assertTrue(vehicleDescriptions.isEmpty());
    }

    @Test
    public void shouldCreateProperCarDescription() {
        // given
        defaultVehicle.setType("car");

        // when
        List<VehicleDescription> vehicleDescriptions = mapper.map(Collections.singletonList(defaultVehicle));

        // then
        assertEquals(1, vehicleDescriptions.size());
        assertTrue(vehicleDescriptions.get(0) instanceof CarDescription);
        CarDescription car = (CarDescription) vehicleDescriptions.get(0);

        assertBasicVehicleFieldsAreEqual(car);
    }

    @Test
    public void shouldCreateProperMotorbikeDescription() {
        // given
        defaultVehicle.setType("scooter");

        // when
        List<VehicleDescription> vehicleDescriptions = mapper.map(Collections.singletonList(defaultVehicle));

        // then
        assertEquals(1, vehicleDescriptions.size());
        assertTrue(vehicleDescriptions.get(0) instanceof MotorbikeDescription);
        MotorbikeDescription motorbike = (MotorbikeDescription) vehicleDescriptions.get(0);

        assertBasicVehicleFieldsAreEqual(motorbike);
    }

    @Test
    public void shouldCreateProperKickScooterDescription() {
        // given
        defaultVehicle.setType("un-pedal-scooter");

        // when
        List<VehicleDescription> vehicleDescriptions = mapper.map(Collections.singletonList(defaultVehicle));

        // then
        assertEquals(1, vehicleDescriptions.size());
        assertTrue(vehicleDescriptions.get(0) instanceof KickScooterDescription);
        KickScooterDescription kickscooter = (KickScooterDescription) vehicleDescriptions.get(0);
        assertBasicVehicleFieldsAreEqual(kickscooter);
    }

    @Test
    public void shouldAllowNullsInSomeFields() {
        // given
        emptyVehicle.setProvider(provider);
        emptyVehicle.setType("car");

        // when
        List<VehicleDescription> vehicleDescriptions = mapper.map(Collections.singletonList(emptyVehicle));

        // then
        assertEquals(1, vehicleDescriptions.size());
        assertTrue(vehicleDescriptions.get(0) instanceof CarDescription);
        CarDescription car = (CarDescription) vehicleDescriptions.get(0);

        assertNull(car.getProviderVehicleId());
        assertNull(car.getFuelType());
        assertNull(car.getGearbox());
        assertEquals(200000.0, car.getRangeInMeters(), 0.1); // default range if fetched null from database
    }

    private void assertBasicVehicleFieldsAreEqual(VehicleDescription vehicleDescription) {
        assertEquals(defaultVehicle.getProvider().getId(), vehicleDescription.getProvider().getProviderId());
        assertEquals(defaultVehicle.getProvider().getName(), vehicleDescription.getProvider().getProviderName());
        assertEquals(defaultVehicle.getProviderVehicleId(), vehicleDescription.getProviderVehicleId());
        assertEquals(defaultVehicle.getLongitude(), vehicleDescription.getLongitude(), 0.1);
        assertEquals(defaultVehicle.getLatitude(), vehicleDescription.getLatitude(), 0.1);
        assertEquals(FuelType.FOSSIL, vehicleDescription.getFuelType());
        assertEquals(Gearbox.MANUAL, vehicleDescription.getGearbox());
        assertEquals(15000.0, vehicleDescription.getRangeInMeters(), 0.1);
    }
}
