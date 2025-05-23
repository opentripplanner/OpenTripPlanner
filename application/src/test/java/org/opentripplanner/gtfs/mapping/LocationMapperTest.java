package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Polygon;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Location;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.api.Issue;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.transit.service.SiteRepository;

class LocationMapperTest {

  private static final IdFactory ID_FACTORY = new IdFactory("A");

  static Stream<Arguments> testCases() {
    return Stream.of(Arguments.of(null, true), Arguments.of("a name", false));
  }

  @ParameterizedTest(name = "a name of <{0}> should set bogusName={1}")
  @MethodSource("testCases")
  void testMapping(String name, boolean isBogusName) {
    var gtfsLocation = getLocation(name, Polygons.OSLO);

    var mapper = new LocationMapper(ID_FACTORY, SiteRepository.of(), DataImportIssueStore.NOOP);
    var flexLocation = mapper.map(gtfsLocation);

    assertEquals(isBogusName, flexLocation.hasFallbackName());
  }

  @Test
  void invalidPolygon() {
    var selfIntersecting = Polygons.SELF_INTERSECTING;
    assertFalse(selfIntersecting.isValid());

    var gtfsLocation = getLocation("invalid", selfIntersecting);

    var issueStore = new DefaultDataImportIssueStore();
    var mapper = new LocationMapper(ID_FACTORY, SiteRepository.of(), issueStore);

    mapper.map(gtfsLocation);

    assertEquals(
      List.of(
        Issue.issue(
          "InvalidFlexAreaGeometry",
          "GTFS flex location A:zone-3 has an invalid geometry: Self-intersection at (lat: 1.0, lon: 2.0)"
        )
      ).toString(),
      issueStore.listIssues().toString()
    );
  }

  private static Location getLocation(String name, Polygon polygon) {
    var gtfsLocation = new Location();
    gtfsLocation.setId(new AgencyAndId("1", "zone-3"));
    gtfsLocation.setName(name);
    gtfsLocation.setGeometry(Polygons.toGeoJson(polygon));
    return gtfsLocation;
  }
}
