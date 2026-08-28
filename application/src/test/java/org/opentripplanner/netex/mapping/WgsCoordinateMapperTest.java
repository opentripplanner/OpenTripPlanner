package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import net.opengis.gml._3.DirectPositionType;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;

class WgsCoordinateMapperTest {

  private static final double EPSILON = 0.01d;

  private static final double LONGITUDE_VALUE = 62.8;
  private static final BigDecimal LONGITUDE = BigDecimal.valueOf(LONGITUDE_VALUE);

  private static final double LATITUDE_VALUE = 11.1;
  private static final BigDecimal LATITUDE = BigDecimal.valueOf(LATITUDE_VALUE);

  @Test
  void handleCoordinatesWithValuesSet() {
    // Given a valid point
    final SimplePoint_VersionStructure point = new SimplePoint_VersionStructure().withLocation(
      new LocationStructure().withLongitude(LONGITUDE).withLatitude(LATITUDE)
    );

    // When map coordinates
    WgsCoordinate c = WgsCoordinateMapper.mapToDomain(point);

    // Then verify coordinate
    assertEquals(LONGITUDE_VALUE, c.longitude(), EPSILON);
    assertEquals(LATITUDE_VALUE, c.latitude(), EPSILON);
  }

  @Test
  void handleCoordinatesWithMissingPoint() {
    assertNull(WgsCoordinateMapper.mapToDomain(null));
  }

  @Test
  void handleCoordinatesWithMissingLocation() {
    SimplePoint_VersionStructure p = new SimplePoint_VersionStructure();
    assertNull(WgsCoordinateMapper.mapToDomain(p));
  }

  @Test
  void handleCoordinatesWithMissingLatitude() {
    SimplePoint_VersionStructure p;
    p = new SimplePoint_VersionStructure().withLocation(
      new LocationStructure().withLongitude(LONGITUDE)
    );

    assertThrows(IllegalArgumentException.class, () -> WgsCoordinateMapper.mapToDomain(p));
  }

  @Test
  void handleCoordinatesWithMissingLongitude() {
    SimplePoint_VersionStructure p;
    p = new SimplePoint_VersionStructure().withLocation(
      new LocationStructure().withLatitude(LATITUDE)
    );
    assertThrows(IllegalArgumentException.class, () -> WgsCoordinateMapper.mapToDomain(p));
  }

  @Test
  void pos() {
    SimplePoint_VersionStructure p;
    p = new SimplePoint_VersionStructure().withLocation(
      new LocationStructure().withPos(
        new DirectPositionType().withValue(LATITUDE_VALUE, LONGITUDE_VALUE)
      )
    );
    var coord = WgsCoordinateMapper.mapToDomain(p);
    assertEquals(LATITUDE_VALUE, coord.latitude(), EPSILON);
    assertEquals(LONGITUDE_VALUE, coord.longitude(), EPSILON);
  }
}
