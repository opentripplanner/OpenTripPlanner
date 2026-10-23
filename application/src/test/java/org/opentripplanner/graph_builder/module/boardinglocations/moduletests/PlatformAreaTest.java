package org.opentripplanner.graph_builder.module.boardinglocations.moduletests;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.graph_builder.module.BoardingLocationCoordinateSource.OSM;
import static org.opentripplanner.graph_builder.module.BoardingLocationCoordinateSource.TRANSIT;
import static org.opentripplanner.osm.model.NodeBuilder.node;

import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.BoardingLocationCoordinateSource;
import org.opentripplanner.graph_builder.module.boardinglocations.BoardingLocationsEnvironment;
import org.opentripplanner.graph_builder.module.boardinglocations.LinkedGraph;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.osm.model.OsmWayBuilder;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.transit.model.site.RegularStop;

/**
 * Linking stops to a platform mapped as an OSM area, with
 * {@link BoardingLocationCoordinateSource#TRANSIT}.
 */
class PlatformAreaTest {

  private static final WgsCoordinate SOUTH_WEST = new WgsCoordinate(53.55, 10.0);

  /**
   * Each stop gets its own vertex at its own coordinate rather than collapsing onto a shared
   * centroid, and the two are connected directly across the platform rather than by a detour.
   */
  @Test
  void stopsOnOnePlatformStayDistinctAndConnected() {
    var test = BoardingLocationsEnvironment.of(TRANSIT, platform(40, 8, "west;east"));
    var westStop = test.stop("west", SOUTH_WEST.moveNorthMeters(4).moveEastMeters(10));
    var eastStop = test.stop("east", SOUTH_WEST.moveNorthMeters(4).moveEastMeters(30));

    var result = test.build();

    // Both are registered as visibility vertices of the platform, so they see each other directly.
    assertWithMessage("Unexpected edges. Check graph at %s", result.geoJsonUrl())
      .that(result.summarizeEdges())
      .containsExactly(
        "(53.55,10) → (53.54982,10) PEDESTRIAN ♿✅",
        "(53.55,10) → (53.550072,10) PEDESTRIAN ♿✅",
        "(53.55,10) → (53.55,10.000606) PEDESTRIAN ♿✅",
        "(53.55,10) → (53.550036,10.000454) PEDESTRIAN ♿✅",
        "(53.55,10) → (53.550036,10.000151) PEDESTRIAN ♿✅",
        "(53.54982,10) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.550072,10) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.55,10.000606) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.550036,10.000454) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.550036,10.000151) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.55,10.000606) → (53.550072,10.000606) PEDESTRIAN ♿✅",
        "(53.550072,10.000606) → (53.55,10.000606) PEDESTRIAN ♿✅",
        "(53.550072,10.000606) → (53.550072,10) PEDESTRIAN ♿✅",
        "(53.550072,10) → (53.550072,10.000606) PEDESTRIAN ♿✅",
        "(53.550036,10.000454)[F:east] linked to (53.550036,10.000454)",
        "(53.550036,10.000454) linked to (53.550036,10.000454)[F:east]",
        "(53.550036,10.000151) → (53.550036,10.000454) PEDESTRIAN ♿✅",
        "(53.550036,10.000151) linked to (53.550036,10.000151)[F:west]",
        "(53.550036,10.000454) → (53.550036,10.000151) PEDESTRIAN ♿✅",
        "(53.550036,10.000151)[F:west] linked to (53.550036,10.000151)"
      );
    // The summary only shows coordinates: the vertex the stop is linked to must be the one that
    // carries the visibility edges, not a co-located one.
    assertWithMessage("the two stops should be connected across the platform")
      .that(result.boardingLocation(westStop).isConnected(result.boardingLocation(eastStop)))
      .isTrue();
    assertThat(result.issueTypes()).isEmpty();
  }

  /**
   * In {@code OSM} mode the stops still collapse onto a single shared centroid vertex - the
   * behaviour the parameter defaults to, and the one {@code TRANSIT} exists to change.
   */
  @Test
  void osmModeSharesOneCentroidBetweenStops() {
    var test = BoardingLocationsEnvironment.of(OSM, platform(40, 8, "west;east"));
    var westStop = test.stop("west", SOUTH_WEST.moveNorthMeters(4).moveEastMeters(10));
    var eastStop = test.stop("east", SOUTH_WEST.moveNorthMeters(4).moveEastMeters(30));

    var result = test.build();

    assertWithMessage("Unexpected edges. Check graph at %s", result.geoJsonUrl())
      .that(result.summarizeEdges())
      .containsExactly(
        "(53.550036,10.000303) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.550036,10.000303) linked to (53.550036,10.000454)[F:east]",
        "(53.550036,10.000303) linked to (53.550036,10.000151)[F:west]",
        "(53.55,10) → (53.550036,10.000303) PEDESTRIAN ♿✅",
        "(53.550036,10.000454)[F:east] linked to (53.550036,10.000303)",
        "(53.550036,10.000151)[F:west] linked to (53.550036,10.000303)",
        "(53.55,10) → (53.54982,10) PEDESTRIAN ♿✅",
        "(53.55,10) → (53.550072,10) PEDESTRIAN ♿✅",
        "(53.55,10) → (53.55,10.000606) PEDESTRIAN ♿✅",
        "(53.54982,10) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.550072,10) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.55,10.000606) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.55,10.000606) → (53.550072,10.000606) PEDESTRIAN ♿✅",
        "(53.550072,10.000606) → (53.55,10.000606) PEDESTRIAN ♿✅",
        "(53.550072,10.000606) → (53.550072,10) PEDESTRIAN ♿✅",
        "(53.550072,10) → (53.550072,10.000606) PEDESTRIAN ♿✅"
      );
    // A second, co-located centroid would print the same coordinates.
    assertWithMessage("both stops should share the platform centroid")
      .that(result.boardingLocation(westStop))
      .isSameInstanceAs(result.boardingLocation(eastStop));
  }

  /**
   * A stop whose coordinate falls just outside the polygon reaches the
   * platform through an access point placed just inside on its behalf, over an edge that costs only
   * the few centimetres it is actually off by - and the access point is as well connected as a
   * boarding location placed inside in the first place.
   */
  @Test
  void aStopJustOutsideReachesThePlatformViaAnAccessPoint() {
    var test = BoardingLocationsEnvironment.of(TRANSIT, platform(40, 8, "inside;outside"));
    var insideStop = test.stop("inside", SOUTH_WEST.moveNorthMeters(4).moveEastMeters(10));
    var outsideStop = test.stop("outside", SOUTH_WEST.moveSouthMeters(0.05).moveEastMeters(30));

    var result = test.build();

    // The access point is as well connected as the boarding location placed inside.
    assertWithMessage("Unexpected edges. Check graph at %s", result.geoJsonUrl())
      .that(result.summarizeEdges())
      .containsExactly(
        "(53.55,10.000454)[F:outside] linked to (53.55,10.000454)",
        "(53.55,10.000454) linked to (53.55,10.000454)[F:outside]",
        "(53.550036,10.000151) → (53.550002,10.000454) PEDESTRIAN ♿✅",
        "(53.550036,10.000151) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.550036,10.000151) linked to (53.550036,10.000151)[F:inside]",
        "(53.550002,10.000454) → (53.550036,10.000151) PEDESTRIAN ♿✅",
        "(53.55,10) → (53.550036,10.000151) PEDESTRIAN ♿✅",
        "(53.550036,10.000151)[F:inside] linked to (53.550036,10.000151)",
        "(53.55,10) → (53.54982,10) PEDESTRIAN ♿✅",
        "(53.55,10) → (53.550072,10) PEDESTRIAN ♿✅",
        "(53.55,10) → (53.55,10.000606) PEDESTRIAN ♿✅",
        "(53.55,10) → (53.550002,10.000454) PEDESTRIAN ♿✅",
        "(53.54982,10) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.550072,10) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.55,10.000606) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.550002,10.000454) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.55,10.000606) → (53.550072,10.000606) PEDESTRIAN ♿✅",
        "(53.550072,10.000606) → (53.55,10.000606) PEDESTRIAN ♿✅",
        "(53.550072,10.000606) → (53.550072,10) PEDESTRIAN ♿✅",
        "(53.550072,10) → (53.550072,10.000606) PEDESTRIAN ♿✅",
        "(53.55,10.000454) → (53.550002,10.000454) PEDESTRIAN ♿✅",
        "(53.550002,10.000454) → (53.55,10.000454) PEDESTRIAN ♿✅"
      );
    // The length is not part of the edge summary above.
    var connector = result.connectors(outsideStop).getFirst();
    // The summary only shows coordinates: the connector must lead to the vertex the area edges
    // start from, not to a co-located one.
    assertWithMessage("the access point should be as well connected as a stop inside the platform")
      .that(result.areaEdgeCount(connector.getToVertex()))
      .isEqualTo(result.areaEdgeCount(result.boardingLocation(insideStop)));
    assertWithMessage("a 5 cm discrepancy should cost a centimetre-scale walk")
      .that(connector.getDistanceMeters())
      .isLessThan(1.0);
    assertThat(result.issueTypes()).isEmpty();
  }

  /**
   * A stop far outside the polygon is a real distance to walk, not a surveying discrepancy, so the
   * edge to its access point carries the true length and the gap is reported.
   */
  @Test
  void aStopFarOutsideKeepsItsDistanceAndIsReported() {
    var test = BoardingLocationsEnvironment.of(TRANSIT, platform(40, 8, "far"));
    var farStop = test.stop("far", SOUTH_WEST.moveSouthMeters(30).moveEastMeters(20));

    var result = test.build();

    assertWithMessage("Unexpected edges. Check graph at %s", result.geoJsonUrl())
      .that(result.summarizeEdges())
      .containsExactly(
        "(53.54973,10.000303)[F:far] linked to (53.54973,10.000303)",
        "(53.54973,10.000303) linked to (53.54973,10.000303)[F:far]",
        "(53.55,10) → (53.54982,10) PEDESTRIAN ♿✅",
        "(53.55,10) → (53.550072,10) PEDESTRIAN ♿✅",
        "(53.55,10) → (53.55,10.000606) PEDESTRIAN ♿✅",
        "(53.55,10) → (53.550002,10.000303) PEDESTRIAN ♿✅",
        "(53.54982,10) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.550072,10) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.55,10.000606) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.550002,10.000303) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.55,10.000606) → (53.550072,10.000606) PEDESTRIAN ♿✅",
        "(53.550072,10.000606) → (53.55,10.000606) PEDESTRIAN ♿✅",
        "(53.550072,10.000606) → (53.550072,10) PEDESTRIAN ♿✅",
        "(53.550072,10) → (53.550072,10.000606) PEDESTRIAN ♿✅",
        "(53.54973,10.000303) → (53.550002,10.000303) PEDESTRIAN ♿✅",
        "(53.550002,10.000303) → (53.54973,10.000303) PEDESTRIAN ♿✅"
      );
    // The length is not part of the edge summary above.
    assertWithMessage("the walk onto the platform should be the real distance")
      .that(result.connectors(farStop).getFirst().getDistanceMeters())
      .isWithin(1)
      .of(30);
    assertThat(result.issueTypes()).contains("StopFarFromBoardingLocation");
  }

  /**
   * On a long narrow platform the line from an outside stop to the platform's interior point is so
   * oblique that it first crosses the boundary tens of metres away. The access point must sit at the
   * foot of the perpendicular instead, next to the stop.
   */
  @Test
  void theAccessPointSitsNextToTheStopOnALongPlatform() {
    var test = BoardingLocationsEnvironment.of(TRANSIT, platform(200, 8, "beside"));
    // 2 m south of a 200 m x 8 m platform, 10 m from its western end.
    var besideStop = test.stop("beside", SOUTH_WEST.moveSouthMeters(2).moveEastMeters(10));

    var result = test.build();

    assertWithMessage("Unexpected edges. Check graph at %s", result.geoJsonUrl())
      .that(result.summarizeEdges())
      .containsExactly(
        "(53.55,10) → (53.54982,10) PEDESTRIAN ♿✅",
        "(53.55,10) → (53.550072,10) PEDESTRIAN ♿✅",
        "(53.55,10) → (53.55,10.003027) PEDESTRIAN ♿✅",
        "(53.55,10) → (53.550002,10.000151) PEDESTRIAN ♿✅",
        "(53.54982,10) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.550072,10) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.55,10.003027) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.550002,10.000151) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.55,10.003027) → (53.550072,10.003027) PEDESTRIAN ♿✅",
        "(53.550072,10.003027) → (53.55,10.003027) PEDESTRIAN ♿✅",
        "(53.549982,10.000151) → (53.550002,10.000151) PEDESTRIAN ♿✅",
        "(53.549982,10.000151) linked to (53.549982,10.000151)[F:beside]",
        "(53.550002,10.000151) → (53.549982,10.000151) PEDESTRIAN ♿✅",
        "(53.549982,10.000151)[F:beside] linked to (53.549982,10.000151)",
        "(53.550072,10.003027) → (53.550072,10) PEDESTRIAN ♿✅",
        "(53.550072,10) → (53.550072,10.003027) PEDESTRIAN ♿✅"
      );
    // The length is not part of the edge summary above.
    assertWithMessage(
      "the access point should sit next to the stop, not where an oblique line crosses the boundary"
    )
      .that(result.connectors(besideStop).getFirst().getDistanceMeters())
      .isWithin(0.3)
      .of(2.2);
  }

  /**
   * The edge to an access point runs across the platform, so it must carry the platform's
   * permission, safety factors and wheelchair accessibility rather than the edge builder's defaults,
   * which would claim a wheelchair-accessible walk across a platform mapped as not accessible.
   */
  @Test
  void theConnectorCarriesThePlatformProperties() {
    var test = BoardingLocationsEnvironment.of(
      TRANSIT,
      platform(40, 8, way ->
        way
          .withTag("public_transport", "platform")
          .withTag("ref", "outside")
          .withTag("wheelchair", "no")
          .withTag("bicycle", "no")
      )
    );
    var outsideStop = test.stop("outside", SOUTH_WEST.moveSouthMeters(3).moveEastMeters(20));

    var result = test.build();

    // The connector to the access point is pedestrian-only and not wheelchair accessible.
    assertWithMessage("Unexpected edges. Check graph at %s", result.geoJsonUrl())
      .that(result.summarizeEdges())
      .containsExactly(
        "(53.549973,10.000303)[F:outside] linked to (53.549973,10.000303)",
        "(53.549973,10.000303) linked to (53.549973,10.000303)[F:outside]",
        "(53.55,10) → (53.54982,10) PEDESTRIAN ♿✅",
        "(53.55,10) → (53.550072,10) PEDESTRIAN ♿❌ noThru=BICYCLE",
        "(53.55,10) → (53.55,10.000606) PEDESTRIAN ♿❌ noThru=BICYCLE",
        "(53.55,10) → (53.550002,10.000303) PEDESTRIAN ♿❌",
        "(53.54982,10) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.550072,10) → (53.55,10) PEDESTRIAN ♿❌ noThru=BICYCLE",
        "(53.55,10.000606) → (53.55,10) PEDESTRIAN ♿❌ noThru=BICYCLE",
        "(53.550002,10.000303) → (53.55,10) PEDESTRIAN ♿❌",
        "(53.55,10.000606) → (53.550072,10.000606) PEDESTRIAN ♿❌ noThru=BICYCLE",
        "(53.550072,10.000606) → (53.55,10.000606) PEDESTRIAN ♿❌ noThru=BICYCLE",
        "(53.550072,10.000606) → (53.550072,10) PEDESTRIAN ♿❌ noThru=BICYCLE",
        "(53.550072,10) → (53.550072,10.000606) PEDESTRIAN ♿❌ noThru=BICYCLE",
        "(53.549973,10.000303) → (53.550002,10.000303) PEDESTRIAN ♿❌",
        "(53.550002,10.000303) → (53.549973,10.000303) PEDESTRIAN ♿❌"
      );
    // The safety factor is not part of the edge summary above.
    assertWithMessage("the connector should inherit the platform's walk safety factor")
      .that(result.connectors(outsideStop).getFirst().getWalkSafetyFactor())
      .isEqualTo(areaEdgeOf(result, outsideStop).getWalkSafetyFactor());
  }

  /** One of the area's own edges, to compare the connector's inherited properties against. */
  private static StreetEdge areaEdgeOf(LinkedGraph result, RegularStop stop) {
    return result
      .attachmentPoints(stop)
      .iterator()
      .next()
      .getOutgoing()
      .stream()
      .filter(AreaEdge.class::isInstance)
      .map(StreetEdge.class::cast)
      .findFirst()
      .orElseThrow();
  }

  private static TestOsmProvider platform(double lengthMetres, double widthMetres, String refs) {
    return platform(lengthMetres, widthMetres, way ->
      way.withTag("public_transport", "platform").withTag("ref", refs)
    );
  }

  /**
   * A rectangular platform area with its south-west corner at {@link #SOUTH_WEST}, plus a footway
   * leading into that corner so the area is connected to the street network.
   */
  private static TestOsmProvider platform(
    double lengthMetres,
    double widthMetres,
    Consumer<OsmWayBuilder> tags
  ) {
    var corner = node(1, SOUTH_WEST);
    return TestOsmProvider.of()
      .addAreaFromNodes(
        tags,
        List.of(
          corner,
          node(2, SOUTH_WEST.moveEastMeters(lengthMetres)),
          node(3, SOUTH_WEST.moveNorthMeters(widthMetres).moveEastMeters(lengthMetres)),
          node(4, SOUTH_WEST.moveNorthMeters(widthMetres))
        )
      )
      .addWayFromNodes(node(5, SOUTH_WEST.moveSouthMeters(20)), corner)
      .build();
  }
}
