package org.opentripplanner.graph_builder.module.boardinglocations.moduletests;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.graph_builder.module.BoardingLocationCoordinateSource.OSM;
import static org.opentripplanner.graph_builder.module.BoardingLocationCoordinateSource.TRANSIT;
import static org.opentripplanner.osm.model.NodeBuilder.node;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.BoardingLocationCoordinateSource;
import org.opentripplanner.graph_builder.module.boardinglocations.BoardingLocationsEnvironment;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.OsmBoardingLocationVertex;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.site.RegularStop;

/** Linking stops to a platform mapped as an OSM way. */
class PlatformWayTest {

  private static final WgsCoordinate WEST_END = new WgsCoordinate(53.55, 10.0);

  /**
   * With {@link BoardingLocationCoordinateSource#TRANSIT} the stop keeps its own coordinate and
   * reaches the way over an edge of the offset. Attaching it to its own projection instead would
   * make that offset free, the stop-to-boarding-location link having no length.
   */
  @Test
  void aStopOffThePlatformKeepsItsOffset() {
    var test = BoardingLocationsEnvironment.of(TRANSIT, platform("off-the-way"));
    var stop = test.stop("off-the-way", WEST_END.moveNorthMeters(12).moveEastMeters(150));

    var result = test.build();

    assertEquals(0, result.distanceFromStop(stop), 0.1, "the stop must not be moved");

    // One connector per traversal direction of the way, each carrying the real offset.
    var connectors = result.connectors(stop);
    assertEquals(2, connectors.size(), "one connector per traversal direction");
    connectors.forEach(c ->
      assertEquals(12, c.getDistanceMeters(), 0.5, "the offset onto the platform costs its length")
    );
    assertThat(result.issueTypes()).contains("StopFarFromBoardingLocation");
  }

  /**
   * A stop attaches to the way at one point, but the forward and back edge are split separately into
   * two co-located vertices. Both must be linked, or the stop is reachable from one end only.
   */
  @Test
  void closeStopsAttachToBothDirectionsAtTheirOwnPoint() {
    var test = BoardingLocationsEnvironment.of(TRANSIT, platform("a;b"));
    var stopA = test.stop("a", WEST_END.moveNorthMeters(1).moveEastMeters(150));
    // 30 m off the line but only 15 cm further along it, which is what triggers the false positive.
    var stopB = test.stop("b", WEST_END.moveNorthMeters(30).moveEastMeters(150.15));

    var result = test.build();

    for (var stop : new RegularStop[] { stopA, stopB }) {
      var attachments = result.attachmentPoints(stop);
      assertEquals(
        2,
        attachments.size(),
        stop.getId() + " should attach to both traversal directions at one point"
      );
      assertEquals(
        1,
        attachments.stream().map(Vertex::getCoordinate).distinct().count(),
        stop.getId() + " should attach at a single point, not fork the platform"
      );

      // Together they reach either end of the platform; linked to one only, the stop is a stub.
      var nextHops = attachments
        .stream()
        .flatMap(v -> v.getOutgoing().stream())
        .filter(StreetEdge.class::isInstance)
        .map(Edge::getToVertex)
        .filter(v -> !(v instanceof OsmBoardingLocationVertex))
        .distinct()
        .count();
      assertEquals(2, nextHops, stop.getId() + " should reach either end of the platform");
    }

    assertNotEquals(
      result.attachmentPoints(stopA).iterator().next().getCoordinate(),
      result.attachmentPoints(stopB).iterator().next().getCoordinate(),
      "each stop should attach at its own point"
    );
  }

  /**
   * Linking splits the platform way, and the platform association is keyed by edge reference. The
   * split halves must be re-registered, or a later stop on the same platform can no longer find it.
   */
  @Test
  void aSecondStopOnTheSamePlatformStillFindsIt() {
    var test = BoardingLocationsEnvironment.of(OSM, platform("a;b"));
    var stopA = test.stop("a", WEST_END.moveEastMeters(100));
    var stopB = test.stop("b", WEST_END.moveEastMeters(200));

    var result = test.build();

    for (var stop : new RegularStop[] { stopA, stopB }) {
      assertFalse(
        result.linkedVertices(stop).isEmpty(),
        stop.getId() + " should be linked to the platform"
      );
    }

    // The freshly created halves carry the platform, so a third stop would find it too. The
    // connector a stop is linked in over is not part of the platform and must not be tagged.
    var osmService = result.osmInfoService();
    var platformHalves = result
      .linkedVertices(stopA)
      .stream()
      .filter(SplitterVertex.class::isInstance)
      .flatMap(v -> Stream.concat(v.getIncoming().stream(), v.getOutgoing().stream()))
      .filter(StreetEdge.class::isInstance)
      .toList();
    assertFalse(platformHalves.isEmpty(), "expected linking to split the platform way");
    assertTrue(
      platformHalves.stream().allMatch(e -> osmService.findPlatform(e).isPresent()),
      "the split halves should be re-registered with the platform"
    );
  }

  /**
   * When linking snaps the stop to an existing endpoint of the platform way instead of splitting it,
   * no split vertex is produced and nothing must be re-registered. An unrelated street
   * sharing that endpoint must not be tagged as part of the platform, which would let a later stop
   * match a platform it is not on.
   */
  @Test
  void snappingToAnEndpointDoesNotTagUnrelatedEdges() {
    var provider = TestOsmProvider.of()
      .addWayFromNodes(
        way -> way.withTag("public_transport", "platform").withTag("ref", "on-the-end"),
        node(1, WEST_END),
        node(2, WEST_END.moveEastMeters(300))
      )
      .addWayFromNodes(node(1, WEST_END), node(3, WEST_END.moveSouthMeters(50)))
      .build();

    var test = BoardingLocationsEnvironment.of(TRANSIT, provider);
    // The stop sits exactly on the platform way's western end, which an unrelated footway shares.
    var stop = test.stop("on-the-end", WEST_END);

    var result = test.build();

    assertFalse(result.linkedVertices(stop).isEmpty(), "the stop should be linked");
    assertEquals(
      2,
      result.platformEdgeCount(),
      "only the platform way's own two directions should carry the platform"
    );
  }

  /**
   * A platform mapped as a way that turns out not to be linkable - here because no edge of it is
   * walkable - must report failure, so the search falls through to the area path instead of leaving
   * the stop unlinked.
   */
  @Test
  void anUnlinkableWayFallsBackToTheAreaPlatform() {
    // The same ref on a car-only platform way and on a platform area beside it.
    var corner = node(1, WEST_END);
    var provider = TestOsmProvider.of()
      .addWayFromNodes(
        way ->
          way
            .withTag("public_transport", "platform")
            .withTag("ref", "both-ways")
            .withTag("access", "no")
            .withTag("motor_vehicle", "permissive"),
        node(10, WEST_END.moveNorthMeters(20)),
        node(11, WEST_END.moveNorthMeters(20).moveEastMeters(40))
      )
      .addAreaFromNodes(
        way -> way.withTag("public_transport", "platform").withTag("ref", "both-ways"),
        List.of(
          corner,
          node(2, WEST_END.moveEastMeters(40)),
          node(3, WEST_END.moveNorthMeters(8).moveEastMeters(40)),
          node(4, WEST_END.moveNorthMeters(8))
        )
      )
      .addWayFromNodes(node(5, WEST_END.moveSouthMeters(20)), corner)
      .build();

    var test = BoardingLocationsEnvironment.of(TRANSIT, provider);
    var stop = test.stop("both-ways", WEST_END.moveNorthMeters(4).moveEastMeters(20));

    var result = test.build();

    assertFalse(
      result.linkedVertices(stop).isEmpty(),
      "the stop should fall back to the area platform, not be left unlinked by the way path"
    );
    assertTrue(
      result.areaEdgeCount(result.boardingLocation(stop)) > 0,
      "the stop should be linked into the platform area"
    );
  }

  /** A 300 m platform way running east from {@link #WEST_END}. */
  private static TestOsmProvider platform(String refs) {
    return TestOsmProvider.of()
      .addWayFromNodes(
        way -> way.withTag("public_transport", "platform").withTag("ref", refs),
        node(1, WEST_END),
        node(2, WEST_END.moveEastMeters(300))
      )
      .build();
  }
}
