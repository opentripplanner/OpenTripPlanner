package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.netex.mapping.MappingSupport.ID_FACTORY;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.opengis.gml._3.AbstractRingPropertyType;
import net.opengis.gml._3.DirectPositionListType;
import net.opengis.gml._3.LinearRingType;
import net.opengis.gml._3.PolygonType;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.FlexibleArea;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.FlexibleStopPlace_VersionStructure;
import org.rutebanken.netex.model.KeyListStructure;
import org.rutebanken.netex.model.KeyValueStructure;
import org.rutebanken.netex.model.MultilingualString;

class FlexStopsMapperTest {

  static final String FLEXIBLE_STOP_PLACE_ID = "RUT:FlexibleStopPlace:1";
  static final String FLEXIBLE_STOP_PLACE_NAME = "Sauda-HentMeg";
  static final String FLEXIBLE_AREA_ID = "RUT:FlexibleArea:1";
  static final List<Double> AREA_POS_LIST = Arrays.asList(
    59.62575084033623,
    6.3023991052849,
    59.62883380609349,
    6.289718020117876,
    59.6346950024935,
    6.293494451572027,
    59.63493377028342,
    6.295211011323889,
    59.638287192982595,
    6.294073790488267,
    59.64753178824841,
    6.311475414973009,
    59.65024392097467,
    6.317762315064251,
    59.6531402366151,
    6.322203913422278,
    59.65512520740007,
    6.327847103606584,
    59.65622305339289,
    6.354024123279035,
    59.67015747207181,
    6.344389931671587,
    59.67371334545938,
    6.353938295291437,
    59.669853904428514,
    6.359452743494389,
    59.659335756554185,
    6.369387333058425,
    59.667522846995965,
    6.393891223516542,
    59.67128498496401,
    6.408267411438566,
    59.671057317147415,
    6.416742925213495,
    59.669940638321414,
    6.423759363199314,
    59.66409644047066,
    6.421935518462957,
    59.662383105889404,
    6.418995909887838,
    59.64933503529621,
    6.391552410854604,
    59.642120082714285,
    6.3725629685993965,
    59.63698730002605,
    6.317783255517423,
    59.62575084033623,
    6.3023991052849
  );

  static final Collection<Double> INVALID_NON_CLOSED_POLYGON = List.of(
    59.62575084033623,
    6.3023991052849,
    59.62883380609349,
    6.289718020117876,
    59.6346950024935,
    6.293494451572027
  );

  static final Collection<Double> INVALID_SELF_INTERSECTING_POLYGON = List.of(
    63.596915083462335,
    10.878374152208456,
    63.65365163120023,
    10.885927252794394,
    63.66835971343224,
    10.878885368213025,
    63.64886239899589,
    10.847544187841429,
    63.64938508072749,
    10.785677008653767,
    63.56025960429534,
    10.535758055643848,
    63.52844559758193,
    10.668967284159471,
    63.59753465537067,
    10.879080809550098,
    63.617069269781574,
    10.88251403708916,
    63.596915083462335,
    10.878374152208456
  );
  private static final KeyListStructure KEY_LIST_UNRESTRICTED_PUBLIC_TRANSPORT_AREAS =
    new KeyListStructure()
      .withKeyValue(
        new KeyValueStructure()
          .withKey("FlexibleStopAreaType")
          .withValue("UnrestrictedPublicTransportAreas")
      );

  private final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();
  private final SiteRepositoryBuilder siteRepositoryBuilder = testModel.siteRepositoryBuilder();

  @Test
  void testMapAreaStop() {
    FlexStopsMapper flexStopsMapper = new FlexStopsMapper(
      ID_FACTORY,
      List.of(),
      siteRepositoryBuilder,
      DataImportIssueStore.NOOP
    );

    FlexibleStopPlace flexibleStopPlace = getFlexibleStopPlace(AREA_POS_LIST);

    StopLocation areaStop = flexStopsMapper.map(flexibleStopPlace);

    assertNotNull(areaStop);
    assertNotNull(areaStop.getGeometry());
    assertEquals(1, areaStop.getGeometry().getNumGeometries());
    var areaStopPolygon = areaStop.getGeometry().getGeometryN(0);

    Coordinate[] coordinates = new Coordinate[AREA_POS_LIST.size() / 2];
    for (int i = 0; i < AREA_POS_LIST.size(); i += 2) {
      coordinates[i / 2] = new Coordinate(AREA_POS_LIST.get(i + 1), AREA_POS_LIST.get(i));
    }
    var geometryFactory = GeometryUtils.getGeometryFactory();
    var ring = geometryFactory.createLinearRing(coordinates);
    var polygon = geometryFactory.createPolygon(ring);
    assertTrue(polygon.equalsTopo(areaStopPolygon));
  }

  @Test
  void testMapInvalidNonClosedAreaStop() {
    AreaStop areaStop = createAreaStop(INVALID_NON_CLOSED_POLYGON);
    assertNull(areaStop);
  }

  @Test
  void testMapInvalidSelfIntersectingAreaStop() {
    AreaStop areaStop = createAreaStop(INVALID_SELF_INTERSECTING_POLYGON);
    assertNull(areaStop);
  }

  @Test
  void testMapGroupStopWithKeyValueOnFlexibleStopPlace() {
    FlexibleStopPlace flexibleStopPlace = getFlexibleStopPlace(AREA_POS_LIST);
    flexibleStopPlace.setKeyList(KEY_LIST_UNRESTRICTED_PUBLIC_TRANSPORT_AREAS);
    assertGroupStopMapping(flexibleStopPlace);
  }

  @Test
  void testMapGroupStopWithKeyValueOnArea() {
    FlexibleStopPlace flexibleStopPlace = getFlexibleStopPlace(AREA_POS_LIST);
    var area = (FlexibleArea) flexibleStopPlace
      .getAreas()
      .getFlexibleAreaOrFlexibleAreaRefOrHailAndRideArea()
      .get(0);
    area.withKeyList(KEY_LIST_UNRESTRICTED_PUBLIC_TRANSPORT_AREAS);
    assertGroupStopMapping(flexibleStopPlace);
  }

  @Test
  void testMapFlexibleStopPlaceMissingStops() {
    FlexibleStopPlace flexibleStopPlace = getFlexibleStopPlace(AREA_POS_LIST);
    flexibleStopPlace.setKeyList(KEY_LIST_UNRESTRICTED_PUBLIC_TRANSPORT_AREAS);

    FlexStopsMapper subject = new FlexStopsMapper(
      ID_FACTORY,
      List.of(),
      siteRepositoryBuilder,
      DataImportIssueStore.NOOP
    );

    GroupStop groupStop = (GroupStop) subject.map(flexibleStopPlace);

    assertNull(groupStop);
  }

  @Test
  void testMapFlexibleStopPlaceWithInvalidGeometryOnUnrestrictedPublicTransportAreas() {
    RegularStop stop1 = testModel.stop("A").withCoordinate(59.6505778, 6.3608759).build();
    RegularStop stop2 = testModel.stop("B").withCoordinate(59.6630333, 6.3697245).build();

    var invalidPolygon = List.of(1.0);
    FlexibleStopPlace flexibleStopPlace = getFlexibleStopPlace(invalidPolygon);
    flexibleStopPlace.setKeyList(KEY_LIST_UNRESTRICTED_PUBLIC_TRANSPORT_AREAS);

    FlexStopsMapper subject = new FlexStopsMapper(
      ID_FACTORY,
      List.of(stop1, stop2),
      siteRepositoryBuilder,
      DataImportIssueStore.NOOP
    );

    GroupStop groupStop = (GroupStop) subject.map(flexibleStopPlace);

    assertNull(groupStop);
  }

  private void assertGroupStopMapping(FlexibleStopPlace flexibleStopPlace) {
    // Regular stop inside the polygon with same transport mode as the flexible stop
    RegularStop stop1 = testModel
      .stop("A")
      .withCoordinate(59.6505778, 6.3608759)
      .withVehicleType(TransitMode.BUS)
      .build();
    // Regular stop outside the polygon with same transport mode as the flexible stop
    RegularStop stop2 = testModel
      .stop("B")
      .withCoordinate(59.6630333, 6.3697245)
      .withVehicleType(TransitMode.BUS)
      .build();
    // Regular stop inside the polygon with another transport mode than the flexible stop
    RegularStop stop3 = testModel
      .stop("A")
      .withCoordinate(59.6505778, 6.3608759)
      .withVehicleType(TransitMode.RAIL)
      .build();

    FlexStopsMapper subject = new FlexStopsMapper(
      ID_FACTORY,
      List.of(stop1, stop2, stop3),
      siteRepositoryBuilder,
      DataImportIssueStore.NOOP
    );

    GroupStop groupStop = (GroupStop) subject.map(flexibleStopPlace);

    assertNotNull(groupStop);

    // Only one of the stops should be inside the polygon
    List<StopLocation> locations = groupStop.getChildLocations();
    assertEquals(1, locations.size());
    assertEquals(stop1.getId(), locations.stream().findFirst().orElseThrow().getId());
  }

  private FlexibleStopPlace getFlexibleStopPlace(Collection<Double> areaPosList) {
    return new FlexibleStopPlace()
      .withId(FLEXIBLE_STOP_PLACE_ID)
      .withName(new MultilingualString().withValue(FLEXIBLE_STOP_PLACE_NAME))
      .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
      .withAreas(
        new FlexibleStopPlace_VersionStructure.Areas()
          .withFlexibleAreaOrFlexibleAreaRefOrHailAndRideArea(
            new FlexibleArea()
              .withId(FLEXIBLE_AREA_ID)
              .withPolygon(
                new PolygonType()
                  .withExterior(
                    new AbstractRingPropertyType()
                      .withAbstractRing(
                        MappingSupport.createJaxbElement(
                          new LinearRingType()
                            .withPosList(new DirectPositionListType().withValue(areaPosList))
                        )
                      )
                  )
              )
          )
      );
  }

  private AreaStop createAreaStop(Collection<Double> polygonCoordinates) {
    FlexStopsMapper flexStopsMapper = new FlexStopsMapper(
      ID_FACTORY,
      List.of(),
      siteRepositoryBuilder,
      DataImportIssueStore.NOOP
    );
    FlexibleStopPlace flexibleStopPlace = getFlexibleStopPlace(polygonCoordinates);
    return (AreaStop) flexStopsMapper.map(flexibleStopPlace);
  }
}
