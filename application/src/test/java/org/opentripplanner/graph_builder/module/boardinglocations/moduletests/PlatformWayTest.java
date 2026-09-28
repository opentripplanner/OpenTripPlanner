package org.opentripplanner.graph_builder.module.boardinglocations.moduletests;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.graph_builder.module.BoardingLocationCoordinateSource.OSM;
import static org.opentripplanner.graph_builder.module.BoardingLocationCoordinateSource.TRANSIT;
import static org.opentripplanner.osm.model.NodeBuilder.node;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.BoardingLocationCoordinateSource;
import org.opentripplanner.graph_builder.module.boardinglocations.BoardingLocationsEnvironment;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
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
   * walks onto the way. Attaching it to its own projection instead would make the offset free, the
   * stop-to-boarding-location link having no length.
   */
  @Test
  void aStopOffThePlatformWalksOntoIt() {
    var test = BoardingLocationsEnvironment.of(TRANSIT, platform("off-the-way"));
    var stop = test.stop("off-the-way", offset(12, 150));

    var result = test.build();

    assertEquals(0, result.distanceFromStop(stop), 0.1, "the stop must not be moved");

    // One connector per traversal direction of the way, each carrying the real offset.
    var connectors = result.connectors(stop);
    assertEquals(2, connectors.size(), "one connector per traversal direction");
    connectors.forEach(c ->
      assertEquals(12, c.getDistanceMeters(), 0.5, "the offset onto the platform is walked")
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
    var stopA = test.stop("a", offset(1, 150));
    // 30 m off the line but only 15 cm further along it, which is what triggers the false positive.
    var stopB = test.stop("b", offset(30, 150.15));

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
    var stopA = test.stop("a", offset(0, 100));
    var stopB = test.stop("b", offset(0, 200));

    var result = test.build();

    for (var stop : new RegularStop[] { stopA, stopB }) {
      assertFalse(
        result.linkedVertices(stop).isEmpty(),
        stop.getId() + " should be linked to the platform"
      );
    }

    // The freshly created halves carry the platform, so a third stop would find it too. The
    // connector a stop walks in over is not part of the platform and must not be tagged.
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
        node(2, offset(0, 300))
      )
      .addWayFromNodes(node(1, WEST_END), node(3, offset(-50, 0)))
      .build();

    var test = BoardingLocationsEnvironment.of(TRANSIT, provider);
    // The stop sits exactly on the platform way's western end, which an unrelated footway shares.
    var stop = test.stop("on-the-end", offset(0, 0));

    var result = test.build();

    assertFalse(result.linkedVertices(stop).isEmpty(), "the stop should be linked");
    assertEquals(
      2,
      result.platformEdgeCount(),
      "only the platform way's own two directions should carry the platform"
    );
  }

  private static WgsCoordinate offset(double metresNorth, double metresEast) {
    return SphericalDistanceLibrary.moveMeters(WEST_END, metresNorth, metresEast);
  }

  /** A 300 m platform way running east from {@link #WEST_END}. */
  private static TestOsmProvider platform(String refs) {
    return TestOsmProvider.of()
      .addWayFromNodes(
        way -> way.withTag("public_transport", "platform").withTag("ref", refs),
        node(1, WEST_END),
        node(2, offset(0, 300))
      )
      .build();
  }
}
