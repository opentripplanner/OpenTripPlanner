package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PointMapperTest {
    private static final double DELTA = 0.01d;

    private static final double LONGITUDE_VALUE = 62.8;
    private static final BigDecimal LONGITUDE = BigDecimal.valueOf(LONGITUDE_VALUE);

    private static final double LATITUDE_VALUE = 11.1;
    private static final BigDecimal LATITUDE = BigDecimal.valueOf(LATITUDE_VALUE);


    @Test public void handleCoordinatesWithValuesSet() {
        // Given a valid point
        final SimplePoint_VersionStructure point = new SimplePoint_VersionStructure()
                .withLocation(
                        new LocationStructure()
                                .withLongitude(LONGITUDE)
                                .withLatitude(LATITUDE)
                );
        // And a coordinate
        final Coord c = new Coord();

        // When map coordinates
        assertTrue(PointMapper.verifyPointAndProcessCoordinate(
                point, p -> {
                        c.lat = p.getLatitude().doubleValue();
                        c.lon = p.getLongitude().doubleValue();
                }
        ));
        // Then verify coordinate
        assertEquals(LONGITUDE_VALUE, c.lon, DELTA);
        assertEquals(LATITUDE_VALUE, c.lat, DELTA);
    }

    @Test public void handleCoordinatesWithMissingPoint() {
        assertFalse(PointMapper.verifyPointAndProcessCoordinate(null, this::failTest));
    }

    @Test public void handleCoordinatesWithMissingLocation() {
        SimplePoint_VersionStructure p = new SimplePoint_VersionStructure();
        assertFalse(PointMapper.verifyPointAndProcessCoordinate(p, this::failTest));
    }

    @Test public void handleCoordinatesWithMissingLatitude() {
        SimplePoint_VersionStructure p;
        p = new SimplePoint_VersionStructure().withLocation(
            new LocationStructure().withLongitude(LONGITUDE)
        );
        assertFalse(PointMapper.verifyPointAndProcessCoordinate(p, this::failTest));
    }

    @Test public void handleCoordinatesWithMissingLongitude() {
        SimplePoint_VersionStructure p;
        p = new SimplePoint_VersionStructure().withLocation(
                new LocationStructure().withLatitude(LATITUDE)
        );
        assertFalse(PointMapper.verifyPointAndProcessCoordinate(p, this::failTest));
    }

    private void failTest(LocationStructure loc) {
        fail("Handler should not be called if an element is missing");
    }

    static class Coord { double lon, lat; }
}