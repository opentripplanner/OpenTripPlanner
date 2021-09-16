package org.opentripplanner.updater.vehicle_rental.datasources;

import org.entur.gbfs.v2_2.free_bike_status.GBFSFreeBikeStatus;
import org.entur.gbfs.v2_2.geofencing_zones.GBFSGeofencingZones;
import org.entur.gbfs.v2_2.station_information.GBFSStation;
import org.entur.gbfs.v2_2.station_information.GBFSStationInformation;
import org.entur.gbfs.v2_2.station_status.GBFSStationStatus;
import org.entur.gbfs.v2_2.system_alerts.GBFSSystemAlerts;
import org.entur.gbfs.v2_2.system_calendar.GBFSSystemCalendar;
import org.entur.gbfs.v2_2.system_hours.GBFSSystemHours;
import org.entur.gbfs.v2_2.system_information.GBFSSystemInformation;
import org.entur.gbfs.v2_2.system_pricing_plans.GBFSSystemPricingPlans;
import org.entur.gbfs.v2_2.system_regions.GBFSSystemRegions;
import org.entur.gbfs.v2_2.vehicle_types.GBFSVehicleType;
import org.entur.gbfs.v2_2.vehicle_types.GBFSVehicleTypes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


/**
 * This tests that {@link GbfsFeedLoader} handles loading of different versions of GBFS correctly, that the optional
 * language paraameter works correctly, and that the different files in a GBFS bundle are all included, with all
 * information in them.
 */
class GbfsFeedLoaderTest {

    public static final String LANGUAGE_NB = "nb";

    @Test
    void getFeedWithExplicitLanguage() {
        GbfsFeedLoader loader = new GbfsFeedLoader(
                "file:src/test/resources/gbfs/gbfs.json",
                Map.of(),
                "nb"
        );

        validateFeed(loader);
    }

    @Test
    void getFeedWithNoLanguage() {
        GbfsFeedLoader loader = new GbfsFeedLoader(
                "file:src/test/resources/gbfs/gbfs.json",
                Map.of(),
                null
        );

        validateFeed(loader);
    }

    @Test
    void getFeedWithWrongLanguage() {
        assertThrows(RuntimeException.class, () -> new GbfsFeedLoader(
                "file:src/test/resources/gbfs/gbfs.json",
                Map.of(),
                "en"
        ));

    }

    private void validateFeed(GbfsFeedLoader loader) {
        assertTrue(loader.update());

        GBFSSystemInformation systemInformation = loader.getFeed(GBFSSystemInformation.class);
        assertNotNull(systemInformation);
        assertEquals("lillestrombysykkel", systemInformation.getData().getSystemId());
        assertEquals(LANGUAGE_NB, systemInformation.getData().getLanguage());
        assertEquals("Lillestrøm bysykkel", systemInformation.getData().getName());
        assertEquals("Europe/Oslo", systemInformation.getData().getTimezone());
        assertNull(systemInformation.getData().getEmail());
        assertNull(systemInformation.getData().getOperator());
        assertNull(systemInformation.getData().getPhoneNumber());
        assertNull(systemInformation.getData().getShortName());
        assertNull(systemInformation.getData().getUrl());


        GBFSVehicleTypes vehicleTypes = loader.getFeed(GBFSVehicleTypes.class);
        assertNotNull(vehicleTypes);
        assertEquals(1, vehicleTypes.getData().getVehicleTypes().size());
        GBFSVehicleType vehicleType = vehicleTypes.getData().getVehicleTypes().get(0);
        assertEquals("YLS:VehicleType:CityBike", vehicleType.getVehicleTypeId());
        assertEquals(GBFSVehicleType.FormFactor.BICYCLE, vehicleType.getFormFactor());
        assertEquals(GBFSVehicleType.PropulsionType.HUMAN, vehicleType.getPropulsionType());
        assertNull(vehicleType.getMaxRangeMeters());


        GBFSStationInformation stationInformation = loader.getFeed(GBFSStationInformation.class);
        assertNotNull(stationInformation);
        List<GBFSStation> stations = stationInformation.getData().getStations();
        assertEquals(6, stations.size());
        assertTrue(stations.stream().anyMatch(gbfsStation -> gbfsStation.getName().equals("TORVGATA")));
        assertEquals(21, stations.stream().mapToDouble(GBFSStation::getCapacity).sum());


        GBFSStationStatus stationStatus = loader.getFeed(GBFSStationStatus.class);
        assertNotNull(stationStatus);
        List<org.entur.gbfs.v2_2.station_status.GBFSStation> stationStatuses = stationStatus.getData().getStations();
        assertEquals(6, stationStatuses.size());

        assertNull(loader.getFeed(GBFSFreeBikeStatus.class));
        assertNull(loader.getFeed(GBFSSystemHours.class));
        assertNull(loader.getFeed(GBFSSystemAlerts.class));
        assertNull(loader.getFeed(GBFSSystemCalendar.class));
        assertNull(loader.getFeed(GBFSSystemRegions.class));

        GBFSSystemPricingPlans pricingPlans = loader.getFeed(GBFSSystemPricingPlans.class);

        assertNotNull(pricingPlans);
        assertEquals(2, pricingPlans.getData().getPlans().size());

        assertNull(loader.getFeed(GBFSGeofencingZones.class));
    }
}