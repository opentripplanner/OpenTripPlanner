package org.opentripplanner.updater.bike_rental.datasources;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.opentripplanner.updater.bike_rental.datasources.GbfsBikeRentalDataSource.GbfsGeofencingZonesDataSource;
import org.opentripplanner.updater.bike_rental.datasources.params.GbfsBikeRentalDataSourceParameters;

class GbfsBikeRentalDataSourceTest {

    @Test
    void parseGeofencingZones() {

        var cwd = System.getProperty("user.dir");
        var url = "file://" + cwd + "/src/test/resources/bike/gbfs/tier-oslo-geofencing-zones.json";

        var params = new GbfsBikeRentalDataSourceParameters(url, "tier-oslo", false, false,
                Collections.emptyMap()
        );
        var source = new GbfsGeofencingZonesDataSource(params);
        source.update();
        
        var zones = source.getGeofencingZones();
        assertFalse(zones.isEmpty());
    }
}