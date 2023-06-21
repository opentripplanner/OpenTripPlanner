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
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
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

  static final Collection<Double> INVALID_AREA_POS_LIST = List.of(
    59.62575084033623,
    6.3023991052849,
    59.62883380609349,
    6.289718020117876,
    59.6346950024935,
    6.293494451572027
  );

  @Test
  void testMapAreaStop() {
    FlexStopsMapper flexStopsMapper = new FlexStopsMapper(
      ID_FACTORY,
      List.of(),
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
  void testMapInvalidAreaStop() {
    FlexStopsMapper flexStopsMapper = new FlexStopsMapper(
      ID_FACTORY,
      List.of(),
      DataImportIssueStore.NOOP
    );

    FlexibleStopPlace flexibleStopPlace = getFlexibleStopPlace(INVALID_AREA_POS_LIST);

    AreaStop areaStop = (AreaStop) flexStopsMapper.map(flexibleStopPlace);

    assertNull(areaStop);
  }

  @Test
  void testMapGroupStop() {
    RegularStop stop1 = TransitModelForTest.stop("A").withCoordinate(59.6505778, 6.3608759).build();
    RegularStop stop2 = TransitModelForTest.stop("B").withCoordinate(59.6630333, 6.3697245).build();

    FlexibleStopPlace flexibleStopPlace = getFlexibleStopPlace(AREA_POS_LIST);
    flexibleStopPlace.setKeyList(
      new KeyListStructure()
        .withKeyValue(
          new KeyValueStructure()
            .withKey("FlexibleStopAreaType")
            .withValue("UnrestrictedPublicTransportAreas")
        )
    );

    FlexStopsMapper subject = new FlexStopsMapper(
      ID_FACTORY,
      List.of(stop1, stop2),
      DataImportIssueStore.NOOP
    );

    GroupStop groupStop = (GroupStop) subject.map(flexibleStopPlace);

    assertNotNull(groupStop);

    // Only one of the stops should be inside the polygon
    assertEquals(1, groupStop.getLocations().size());
  }

  @Test
  void testMapGroupStopVariantWithKeyValueOnArea() {
    RegularStop stop1 = TransitModelForTest.stop("A").withCoordinate(59.6505778, 6.3608759).build();
    RegularStop stop2 = TransitModelForTest.stop("B").withCoordinate(59.6630333, 6.3697245).build();

    FlexibleStopPlace flexibleStopPlace = getFlexibleStopPlace(AREA_POS_LIST);

    var area = (FlexibleArea) flexibleStopPlace
      .getAreas()
      .getFlexibleAreaOrFlexibleAreaRefOrHailAndRideArea()
      .get(0);
    area.withKeyList(
      new KeyListStructure()
        .withKeyValue(
          new KeyValueStructure()
            .withKey("FlexibleStopAreaType")
            .withValue("UnrestrictedPublicTransportAreas")
        )
    );

    FlexStopsMapper subject = new FlexStopsMapper(
      ID_FACTORY,
      List.of(stop1, stop2),
      DataImportIssueStore.NOOP
    );

    GroupStop groupStop = (GroupStop) subject.map(flexibleStopPlace);

    assertNotNull(groupStop);

    // Only one of the stops should be inside the polygon
    assertEquals(1, groupStop.getLocations().size());
  }

  @Test
  void testMapFlexibleStopPlaceMissingStops() {
    FlexibleStopPlace flexibleStopPlace = getFlexibleStopPlace(AREA_POS_LIST);
    flexibleStopPlace.setKeyList(
      new KeyListStructure()
        .withKeyValue(
          new KeyValueStructure()
            .withKey("FlexibleStopAreaType")
            .withValue("UnrestrictedPublicTransportAreas")
        )
    );

    FlexStopsMapper subject = new FlexStopsMapper(ID_FACTORY, List.of(), DataImportIssueStore.NOOP);

    GroupStop groupStop = (GroupStop) subject.map(flexibleStopPlace);

    assertNull(groupStop);
  }

  @Test
  void testMapFlexibleStopPlaceWithInvalidGeometryOnUnrestrictedPublicTransportAreas() {
    RegularStop stop1 = TransitModelForTest.stop("A").withCoordinate(59.6505778, 6.3608759).build();
    RegularStop stop2 = TransitModelForTest.stop("B").withCoordinate(59.6630333, 6.3697245).build();

    var invalidPolygon = List.of(1.0);
    FlexibleStopPlace flexibleStopPlace = getFlexibleStopPlace(invalidPolygon);
    flexibleStopPlace.setKeyList(
      new KeyListStructure()
        .withKeyValue(
          new KeyValueStructure()
            .withKey("FlexibleStopAreaType")
            .withValue("UnrestrictedPublicTransportAreas")
        )
    );

    FlexStopsMapper subject = new FlexStopsMapper(
      ID_FACTORY,
      List.of(stop1, stop2),
      DataImportIssueStore.NOOP
    );

    GroupStop groupStop = (GroupStop) subject.map(flexibleStopPlace);

    assertNull(groupStop);
  }

  private FlexibleStopPlace getFlexibleStopPlace(Collection<Double> areaPosList) {
    return new FlexibleStopPlace()
      .withId(FLEXIBLE_STOP_PLACE_ID)
      .withName(new MultilingualString().withValue(FLEXIBLE_STOP_PLACE_NAME))
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
}
