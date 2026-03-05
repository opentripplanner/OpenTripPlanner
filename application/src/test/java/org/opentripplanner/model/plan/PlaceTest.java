package org.opentripplanner.model.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.leg.ViaLocationType;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.vertex.SimpleVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.SiteRepository;

public class PlaceTest {

  private static final Geometry GEOMETRY = GeometryUtils.getGeometryFactory().createPoint(
    new Coordinate(11, 60)
  );
  private static final NonLocalizedString DEFAULT_PLACE_NAME = NonLocalizedString.ofNullable(
    "defaultName"
  );
  public static final String PLACE_NAME = "PLACE_NAME";

  @Test
  public void sameLocationBasedOnInstance() {
    Place aPlace = Place.normal(60.0, 10.0, new NonLocalizedString("A Place"));
    assertTrue(aPlace.sameLocation(aPlace), "same instance");
  }

  @Test
  public void sameLocationBasedOnCoordinates() {
    Place aPlace = Place.normal(60.0, 10.0, new NonLocalizedString("A Place"));
    Place samePlace = Place.normal(
      60.000000000001,
      10.0000000000001,
      new NonLocalizedString("Same Place")
    );
    Place otherPlace = Place.normal(65.0, 14.0, new NonLocalizedString("Other Place"));

    assertTrue(aPlace.sameLocation(samePlace), "same place");
    assertTrue(samePlace.sameLocation(aPlace), "same place(symmetric)");
    assertFalse(aPlace.sameLocation(otherPlace), "other place");
    assertFalse(otherPlace.sameLocation(aPlace), "other place(symmetric)");
  }

  @Test
  public void sameLocationBasedOnStopId() {
    var testModel = TimetableRepositoryForTest.of();
    var s1 = testModel.stop("1").withCoordinate(1.0, 1.0).build();
    var s2 = testModel.stop("2").withCoordinate(1.0, 2.0).build();

    Place aPlace = place(s1);
    Place samePlace = place(s1);
    Place otherPlace = place(s2);

    assertTrue(aPlace.sameLocation(samePlace), "same place");
    assertTrue(samePlace.sameLocation(aPlace), "same place(symmetric)");
    assertFalse(aPlace.sameLocation(otherPlace), "other place");
    assertFalse(otherPlace.sameLocation(aPlace), "other place(symmetric)");
  }

  static Stream<Arguments> flexStopCases() {
    return Stream.of(
      Arguments.of(null, "an intersection name"),
      Arguments.of(new NonLocalizedString("1:stop_id"), "an intersection name (part of 1:stop_id)"),
      Arguments.of(
        new NonLocalizedString("Flex Zone 123"),
        "an intersection name (part of Flex Zone 123)"
      )
    );
  }

  @ParameterizedTest(name = "Flex stop name of {0} should lead to a place name of {1}")
  @MethodSource("flexStopCases")
  public void flexStop(I18NString stopName, String expectedPlaceName) {
    var stop = SiteRepository.of()
      .areaStop(new FeedScopedId("1", "stop_id"))
      .withGeometry(GEOMETRY)
      .withName(stopName)
      .build();

    var vertex = new SimpleVertex("corner", 1, 1) {
      @Override
      public I18NString getIntersectionName() {
        return new NonLocalizedString("an intersection name");
      }
    };

    var place = Place.forFlexStop(stop, vertex);

    assertEquals(expectedPlaceName, place.name.toString());
  }

  @Test
  public void acceptsNullCoordinates() {
    var p = Place.normal(null, null, new NonLocalizedString("Test"));
    assertNull(p.coordinate);
  }

  @Test
  public void normalVertex() {
    var coordinate = new Coordinate(1, 0);
    var name = new NonLocalizedString("Test");
    Place place = Place.normal(new TemporaryStreetLocation(coordinate, name), name);
    assertEquals(coordinate, place.coordinate.asJtsCoordinate());
    assertEquals(name, place.name);
    assertEquals(VertexType.NORMAL, place.vertexType);
    assertNull(place.viaLocationType);
    assertNull(place.stop);
    assertNull(place.vehicleRentalPlace);
    assertNull(place.vehicleParkingWithEntrance);
  }

  @Test
  public void normalVertexWithViaCoordinateType() {
    var coordinate = new Coordinate(1, 0);
    var name = new NonLocalizedString("Test");
    var type = ViaLocationType.VISIT;
    Place place = Place.normal(new TemporaryStreetLocation(coordinate, name), name, type);
    assertEquals(coordinate, place.coordinate.asJtsCoordinate());
    assertEquals(name, place.name);
    assertEquals(VertexType.NORMAL, place.vertexType);
    assertEquals(type, place.viaLocationType);
    assertNull(place.stop);
    assertNull(place.vehicleRentalPlace);
    assertNull(place.vehicleParkingWithEntrance);
  }

  @Test
  public void forStop() {
    var testModel = TimetableRepositoryForTest.of();
    var stop = testModel.stop("1").withCoordinate(1.0, 1.0).build();

    Place place = Place.forStop(stop);
    assertEquals(stop, place.stop);
    assertEquals(stop.getName(), place.name);
    assertEquals(VertexType.TRANSIT, place.vertexType);
    assertEquals(stop.getCoordinate(), place.coordinate);
    assertNull(place.viaLocationType);
    assertNull(place.vehicleRentalPlace);
    assertNull(place.vehicleParkingWithEntrance);
  }

  @Test
  public void forStopWithViaLocationType() {
    var testModel = TimetableRepositoryForTest.of();
    var stop = testModel.stop("1").withCoordinate(1.0, 1.0).build();
    var type = ViaLocationType.PASS_THROUGH;

    Place place = Place.forStop(stop, type);
    assertEquals(stop, place.stop);
    assertEquals(stop.getName(), place.name);
    assertEquals(VertexType.TRANSIT, place.vertexType);
    assertEquals(stop.getCoordinate(), place.coordinate);
    assertEquals(type, place.viaLocationType);
    assertNull(place.vehicleRentalPlace);
    assertNull(place.vehicleParkingWithEntrance);
  }

  @Test
  void forGenericLocation() {
    GenericLocation location = GenericLocation.fromStopId(PLACE_NAME, "id", "stopId");
    Place place = Place.forGenericLocation(location, DEFAULT_PLACE_NAME);
    assertNotNull(place);
    assertEquals(PLACE_NAME, place.name.toString());
  }

  @Test
  void forNullGenericLocation() {
    Place place = Place.forGenericLocation(null, DEFAULT_PLACE_NAME);
    assertNotNull(place);
    assertNull(place.coordinate);
    assertEquals(DEFAULT_PLACE_NAME, place.name);
  }

  private static Place place(RegularStop stop) {
    return Place.forStop(stop);
  }
}
