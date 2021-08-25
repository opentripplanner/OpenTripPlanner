package org.opentripplanner.updater.bike_rental.datasources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.bike_rental.GeofencingZones;
import org.opentripplanner.updater.bike_rental.datasources.GbfsBikeRentalDataSource.GbfsGeofencingZonesDataSource;
import org.opentripplanner.updater.bike_rental.datasources.params.GbfsBikeRentalDataSourceParameters;

class GbfsBikeRentalDataSourceTest {

    @Test
    void parseOsloGeofencingZones() {

        // if you want to see the geojson in the test file on a map visit https://gist.github.com/leonardehrenfried/a1964e20e83faf8d56038480cb82e8f8

        var cwd = System.getProperty("user.dir");
        var url = "file://" + cwd + "/src/test/resources/bike/gbfs/tier-oslo-geofencing-zones.json";

        var params = new GbfsBikeRentalDataSourceParameters(url, "tier-oslo", false, false,
                Collections.emptyMap()
        );
        var source = new GbfsGeofencingZonesDataSource(params);
        source.update();

        var zones = source.getGeofencingZones();
        assertEquals(zones.size(), 243);

        // some place in the north of Oslo should be allowed
        assertTrue(zones.canDropOffVehicle(new Coordinate(10.6918, 59.9489)));

        // another place outside the city should not be allowed (outside business area)
        assertFalse(zones.canDropOffVehicle(new Coordinate(10.1857, 59.7391)));

        // dropping off in Frogner Park (inside business area but inside a special exclusion zone) should not be allowed
        assertFalse(zones.canDropOffVehicle(new Coordinate( 10.7036, 59.9277)));

        var envelope = zones.getEnvelope();
        assertEquals(envelope.getMinX(), 10.625752);
        assertEquals(envelope.getMaxX(), 10.852622);
        assertEquals(envelope.getMinY(), 59.860121);
        assertEquals(envelope.getMaxY(), 59.969796);
    }

    @Test
    void emptyZones(){
        var zones = new GeofencingZones(Collections.emptySet());
        // if no rules are provided you're allowed to take the bike anywhere
        zones.canDropOffVehicle(new Coordinate(11.1024, 59.2820));
        zones.canDropOffVehicle(new Coordinate(0, 0));
    }
}