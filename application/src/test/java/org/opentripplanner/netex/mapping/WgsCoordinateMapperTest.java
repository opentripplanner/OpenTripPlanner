package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import net.opengis.gml._3.DirectPositionType;
import org.junit.jupiter.api.Test;
import org.opentripplanner.netex.mapping.WgsCoordinateMapper.InvalidCoordinateException;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;

class WgsCoordinateMapperTest {

  private static final double EPSILON = 0.01d;

  private static final double LONGITUDE_VALUE = 62.8;
  private static final BigDecimal LONGITUDE = BigDecimal.valueOf(LONGITUDE_VALUE);

  private static final double LATITUDE_VALUE = 11.1;
  private static final BigDecimal LATITUDE = BigDecimal.valueOf(LATITUDE_VALUE);

  /** A latitude which lost its decimal point, as seen in real data. */
  private static final BigDecimal LATITUDE_OUT_OF_RANGE = BigDecimal.valueOf(56162332.0);
  private static final BigDecimal LONGITUDE_OUT_OF_RANGE = BigDecimal.valueOf(-180.1);

  @Test
  void handleCoordinatesWithValuesSet() throws InvalidCoordinateException {
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
  void handleCoordinatesWithMissingPoint() throws InvalidCoordinateException {
    assertNull(WgsCoordinateMapper.mapToDomain(null));
  }

  @Test
  void handleCoordinatesWithMissingLocation() throws InvalidCoordinateException {
    SimplePoint_VersionStructure p = new SimplePoint_VersionStructure();
    assertNull(WgsCoordinateMapper.mapToDomain(p));
  }

  @Test
  void handleCoordinatesWithMissingLatitude() {
    SimplePoint_VersionStructure p;
    p = new SimplePoint_VersionStructure().withLocation(
      new LocationStructure().withLongitude(LONGITUDE)
    );

    assertThrows(InvalidCoordinateException.class, () -> WgsCoordinateMapper.mapToDomain(p));
  }

  @Test
  void handleCoordinatesWithMissingLongitude() {
    var p = new SimplePoint_VersionStructure().withLocation(
      new LocationStructure().withLatitude(LATITUDE)
    );
    assertThrows(InvalidCoordinateException.class, () -> WgsCoordinateMapper.mapToDomain(p));
  }

  @Test
  void handleLatitudeOutOfRange() {
    var p = new SimplePoint_VersionStructure().withLocation(
      new LocationStructure().withLongitude(LONGITUDE).withLatitude(LATITUDE_OUT_OF_RANGE)
    );
    assertThrows(InvalidCoordinateException.class, () -> WgsCoordinateMapper.mapToDomain(p));
  }

  @Test
  void handleLongitudeOutOfRange() {
    var p = new SimplePoint_VersionStructure().withLocation(
      new LocationStructure().withLongitude(LONGITUDE_OUT_OF_RANGE).withLatitude(LATITUDE)
    );
    assertThrows(InvalidCoordinateException.class, () -> WgsCoordinateMapper.mapToDomain(p));
  }

  @Test
  void pos2Numbers() throws InvalidCoordinateException {
    var coord = WgsCoordinateMapper.mapToDomain(
      new SimplePoint_VersionStructure().withLocation(
        new LocationStructure().withPos(
          new DirectPositionType().withValue(LATITUDE_VALUE, LONGITUDE_VALUE)
        )
      )
    );
    assertEquals(LATITUDE_VALUE, coord.latitude(), EPSILON);
    assertEquals(LONGITUDE_VALUE, coord.longitude(), EPSILON);
  }

  @Test
  void pos3Numbers() throws InvalidCoordinateException {
    SimplePoint_VersionStructure p;
    p = new SimplePoint_VersionStructure().withLocation(
      new LocationStructure().withPos(
        new DirectPositionType().withValue(LATITUDE_VALUE, LONGITUDE_VALUE, 100d)
      )
    );
    var coord = WgsCoordinateMapper.mapToDomain(p);
    assertEquals(LATITUDE_VALUE, coord.latitude(), EPSILON);
    assertEquals(LONGITUDE_VALUE, coord.longitude(), EPSILON);
  }

  @Test
  void pos0Numbers() {
    var p = new SimplePoint_VersionStructure().withLocation(
      new LocationStructure().withPos(new DirectPositionType())
    );

    assertThrows(InvalidCoordinateException.class, () -> WgsCoordinateMapper.mapToDomain(p));
  }

  @Test
  void posOutOfRange() {
    var p = new SimplePoint_VersionStructure().withLocation(
      new LocationStructure().withPos(
        new DirectPositionType().withValue(LATITUDE_OUT_OF_RANGE.doubleValue(), LONGITUDE_VALUE)
      )
    );
    assertThrows(InvalidCoordinateException.class, () -> WgsCoordinateMapper.mapToDomain(p));
  }
}
