package org.opentripplanner.street.linking;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.service.vehiclerental.GeofencingZoneService;
import org.opentripplanner.street.Scope;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.model.StreetConstants;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;

/**
 * Links random points into a dense synthetic street grid and checks against a brute-force oracle
 * that the linker always connects to a closest traversable edge. The grid spans several spatial
 * index cells, so the candidate collection has to handle edges reported from more than one cell.
 */
class VertexLinkerNearbyEdgesTest {

  private static final double ON_EDGE_TOLERANCE_DEG = SphericalDistanceLibrary.metersToDegrees(
    0.01
  );
  private static final double DUPLICATE_WAY_EPSILON_DEG = SphericalDistanceLibrary.metersToDegrees(
    0.001
  );

  private final DenseStreetGridFixture fixture = new DenseStreetGridFixture(
    10.74,
    59.91,
    30,
    40,
    30,
    6,
    10
  );

  @ParameterizedTest
  @EnumSource(value = Scope.class, names = { "REQUEST", "REALTIME" })
  void linksToAClosestTraversableEdge(Scope scope) {
    var linker = new VertexLinker(
      fixture.graph,
      GeofencingZoneService.EMPTY,
      VisibilityMode.COMPUTE_AREA_VISIBILITY_LINES,
      StreetConstants.DEFAULT_MAX_AREA_NODES,
      false
    );
    var modes = new TraverseModeSet(TraverseMode.WALK);
    var random = new Random(42);
    int linked = 0;

    for (int i = 0; i < 300; i++) {
      var coordinate = fixture.randomPoint(random);
      var location = new TemporaryStreetLocation(coordinate, I18NString.of("p" + i));
      double xscale = DenseStreetGridFixture.xscale(coordinate.y);

      List<StreetVertex> linkedTo = new ArrayList<>();
      var disposable =
        scope == Scope.REQUEST
          ? linker.linkVertexForRequest(
              location,
              modes,
              LinkingDirection.BIDIRECTIONAL,
              (v, sv) -> {
                linkedTo.add(sv);
                return List.<Edge>of(
                  TemporaryFreeEdge.createTemporaryFreeEdge((TemporaryStreetLocation) v, sv)
                );
              }
            )
          : linker.linkVertexForRealTime(
              location,
              modes,
              LinkingDirection.BIDIRECTIONAL,
              (v, sv) -> {
                linkedTo.add(sv);
                return List.<Edge>of(
                  TemporaryFreeEdge.createTemporaryFreeEdge((TemporaryStreetLocation) v, sv)
                );
              }
            );

      // Oracle: closest traversable edges by the same projected distance the linker uses
      double best = Double.POSITIVE_INFINITY;
      for (StreetEdge e : fixture.edges) {
        if (e.canTraverse(modes)) {
          best = Math.min(
            best,
            e.squaredEquirectangularDistanceToPoint(coordinate.x, coordinate.y, xscale)
          );
        }
      }
      double band = Math.sqrt(best) + DUPLICATE_WAY_EPSILON_DEG;
      List<StreetEdge> closest = fixture.edges
        .stream()
        .filter(e -> e.canTraverse(modes))
        .filter(
          e ->
            e.squaredEquirectangularDistanceToPoint(coordinate.x, coordinate.y, xscale) <=
            band * band
        )
        .toList();

      assertFalse(linkedTo.isEmpty(), "point " + coordinate + " was not linked");
      for (StreetVertex sv : linkedTo) {
        boolean onClosest = closest
          .stream()
          .anyMatch(
            e ->
              e.squaredEquirectangularDistanceToPoint(sv.getLon(), sv.getLat(), xscale) <
              ON_EDGE_TOLERANCE_DEG * ON_EDGE_TOLERANCE_DEG
          );
        assertTrue(onClosest, "linked vertex " + sv + " is not on a closest edge of " + coordinate);
      }
      linked++;
      disposable.disposeEdges();
    }
    assertTrue(linked == 300);
  }
}
