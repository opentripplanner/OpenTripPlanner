package org.opentripplanner.graph_builder.module.boardinglocations.moduletests;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetTraversalPermission;
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
    var westStop = test.stop("west", offset(4, 10));
    var eastStop = test.stop("east", offset(4, 30));

    var result = test.build();
    var west = result.boardingLocation(westStop);
    var east = result.boardingLocation(eastStop);

    assertNotSame(west, east, "each stop should get its own boarding location");
    assertEquals(0, result.distanceFromStop(westStop), 0.1);
    assertEquals(0, result.distanceFromStop(eastStop), 0.1);

    // Both are registered as visibility vertices of the platform, so they see each other directly.
    assertTrue(west.isConnected(east), "the two stops should be connected across the platform");
    assertThat(result.issueTypes()).isEmpty();
  }

  /**
   * In {@code OSM} mode the stops still collapse onto a single shared centroid vertex - the
   * behaviour the parameter defaults to, and the one {@code TRANSIT} exists to change.
   */
  @Test
  void osmModeSharesOneCentroidBetweenStops() {
    var test = BoardingLocationsEnvironment.of(OSM, platform(40, 8, "west;east"));
    var westStop = test.stop("west", offset(4, 10));
    var eastStop = test.stop("east", offset(4, 30));

    var result = test.build();

    assertEquals(
      result.boardingLocation(westStop),
      result.boardingLocation(eastStop),
      "both stops should share the platform centroid"
    );
  }

  /**
   * A stop whose coordinate falls just outside the polygon reaches the
   * platform through an access point placed just inside on its behalf, over an edge that costs only
   * the few centimetres it is actually off by - and the access point is as well connected as a
   * boarding location placed inside in the first place.
   */
  @Test
  void aStopJustOutsideWalksOntoThePlatform() {
    var test = BoardingLocationsEnvironment.of(TRANSIT, platform(40, 8, "inside;outside"));
    var insideStop = test.stop("inside", offset(4, 10));
    var outsideStop = test.stop("outside", offset(-0.05, 30));

    var result = test.build();

    assertEquals(0, result.distanceFromStop(outsideStop), 0.1, "the stop must not be moved");

    var connectors = result.connectors(outsideStop);
    assertEquals(1, connectors.size(), "one edge onto the platform");
    assertTrue(
      connectors.getFirst().getDistanceMeters() < 1,
      "a 5 cm discrepancy should cost a centimetre-scale walk, but was " +
        connectors.getFirst().getDistanceMeters()
    );

    var accessPoint = connectors.getFirst().getToVertex();
    assertEquals(
      result.areaEdgeCount(result.boardingLocation(insideStop)),
      result.areaEdgeCount(accessPoint),
      "the access point should be as well connected as a stop inside the platform"
    );
    assertThat(result.issueTypes()).isEmpty();
  }

  /**
   * A stop far outside the polygon is a real distance to walk, not a surveying discrepancy, so the
   * edge to its access point carries the true length and the gap is reported.
   */
  @Test
  void aStopFarOutsideKeepsItsDistanceAndIsReported() {
    var test = BoardingLocationsEnvironment.of(TRANSIT, platform(40, 8, "far"));
    var farStop = test.stop("far", offset(-30, 20));

    var result = test.build();

    assertEquals(0, result.distanceFromStop(farStop), 0.1, "the stop must not be moved");
    assertEquals(
      30,
      result.connectors(farStop).getFirst().getDistanceMeters(),
      1,
      "the walk onto the platform should be the real distance"
    );
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
    var besideStop = test.stop("beside", offset(-2, 10));

    var result = test.build();

    assertEquals(
      2.2,
      result.connectors(besideStop).getFirst().getDistanceMeters(),
      0.3,
      "the access point should sit next to the stop, not where an oblique line crosses the boundary"
    );
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
    var outsideStop = test.stop("outside", offset(-3, 20));

    var result = test.build();
    var connector = result.connectors(outsideStop).getFirst();

    assertFalse(
      connector.isWheelchairAccessible(),
      "a platform tagged wheelchair=no must not produce an accessible connector"
    );
    assertFalse(
      connector.getPermission().allows(StreetTraversalPermission.BICYCLE),
      "a platform tagged bicycle=no must not produce a cyclable connector"
    );
    assertEquals(
      areaEdgeOf(result, outsideStop).getWalkSafetyFactor(),
      connector.getWalkSafetyFactor(),
      "the connector should inherit the platform's walk safety factor"
    );
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

  /** A coordinate {@code metresNorth}/{@code metresEast} of the platform's south-west corner. */
  private static WgsCoordinate offset(double metresNorth, double metresEast) {
    return SphericalDistanceLibrary.moveMeters(SOUTH_WEST, metresNorth, metresEast);
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
          node(2, offset(0, lengthMetres)),
          node(3, offset(widthMetres, lengthMetres)),
          node(4, offset(widthMetres, 0))
        )
      )
      .addWayFromNodes(node(5, offset(-20, 0)), corner)
      .build();
  }
}
