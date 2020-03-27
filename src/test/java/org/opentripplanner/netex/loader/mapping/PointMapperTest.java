package org.opentripplanner.netex.loader.mapping;

import org.junit.Test;
import org.opentripplanner.model.WgsCoordinate;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

        // When map coordinates
        WgsCoordinate c = PointMapper.mapCoordinate(point);

        // Then verify coordinate
        assertEquals(LONGITUDE_VALUE, c.longitude(), DELTA);
        assertEquals(LATITUDE_VALUE, c.latitude(), DELTA);
    }

    @Test public void handleCoordinatesWithMissingPoint() {
        assertNull(PointMapper.mapCoordinate(null));
    }

    @Test public void handleCoordinatesWithMissingLocation() {
        SimplePoint_VersionStructure p = new SimplePoint_VersionStructure();
        assertNull(PointMapper.mapCoordinate(p));
    }

    @Test(expected = IllegalArgumentException.class)
    public void handleCoordinatesWithMissingLatitude() {
        SimplePoint_VersionStructure p;
        p = new SimplePoint_VersionStructure().withLocation(
            new LocationStructure().withLongitude(LONGITUDE)
        );
        PointMapper.mapCoordinate(p);
    }

    @Test(expected = IllegalArgumentException.class)
    public void handleCoordinatesWithMissingLongitude() {
        SimplePoint_VersionStructure p;
        p = new SimplePoint_VersionStructure().withLocation(
                new LocationStructure().withLatitude(LATITUDE)
        );
        PointMapper.mapCoordinate(p);
    }
}