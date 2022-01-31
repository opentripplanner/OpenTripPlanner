package org.opentripplanner.ext.vehicleparking.parkapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

public class ParkAPIUpdaterTest {

    @Test
    void parseCars() {
        var url = "file:src/ext-test/resources/vehicleparking/parkapi/parkapi-reutlingen.json";

        var parameters =
                new ParkAPIUpdaterParameters("", url, "park-api", 30, null, List.of(), null);
        var updater = new CarParkAPIUpdater(parameters);

        assertTrue(updater.update());
        var parkingLots = updater.getUpdates();

        assertEquals(30, parkingLots.size());

        var first = parkingLots.get(0);
        assertEquals("Parkplatz Alenberghalle", first.getName().toString());
        assertTrue(first.hasAnyCarPlaces());
        assertNull(first.getCapacity());

        var last = parkingLots.get(29);
        assertEquals("Zehntscheuer Kegelgraben", last.getName().toString());
        assertTrue(last.hasAnyCarPlaces());
        assertTrue(last.hasWheelchairAccessibleCarPlaces());
        assertEquals(1, last.getCapacity().getWheelchairAccessibleCarSpaces());
    }

}
