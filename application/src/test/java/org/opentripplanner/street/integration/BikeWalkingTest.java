package org.opentripplanner.street.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitEntranceVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.strategy.EuclideanRemainingWeightHeuristic;

/**
 * Test switching between biking / walking over multiple edges.
 * <p>
 * arriveBy and departAt paths should be symmetric.
 */
public class BikeWalkingTest extends GraphRoutingTest {

  private TransitStopVertex S1, S2;
  private TransitEntranceVertex E1;
  private StreetVertex A, B, C, D, E, F, Q;
  private StreetEdge AB, BC, CD, DE, EF;

  @Test
  public void testWalkOnly() {
    assertWalkPath(
      C,
      F,
      "null - 0 / 0.0 - null",
      "WALK - 10 / 20.0 - CD street",
      "WALK - 10 / 20.0 - DE street",
      "WALK - 10 / 20.0 - EF street"
    );
  }

  // An initial cost should not be applied when biking all the way.
  @Test
  public void testBikeOnly() {
    assertBikePath(
      C,
      F,
      "null - 0 / 0.0 - null",
      "BICYCLE - 5 / 10.0 - CD street",
      "BICYCLE - 5 / 10.0 - DE street",
      "BICYCLE - 5 / 10.0 - EF street"
    );
  }

  // An cost should be applied when both dismounting and mounting the bike.
  @Test
  public void testMiddleWalkBike() {
    AB.setPermission(StreetTraversalPermission.BICYCLE);
    BC.setPermission(StreetTraversalPermission.PEDESTRIAN);
    CD.setPermission(StreetTraversalPermission.PEDESTRIAN);
    DE.setPermission(StreetTraversalPermission.BICYCLE);

    assertPath(
      A,
      E,
      StreetMode.BIKE,
      List.of(
        "null - 0 / 0.0 - null",
        "BICYCLE - 5 / 10.0 - AB street",
        "🚲WALK - 120 / 1100.0 - BC street",
        "🚲WALK - 20 / 100.0 - CD street",
        "BICYCLE - 105 / 1010.0 - DE street"
      ),
      List.of(
        "null - 0 / 0.0 - null",
        "BICYCLE - 105 / 1010.0 - AB street",
        "🚲WALK - 20 / 100.0 - BC street",
        "🚲WALK - 120 / 1100.0 - CD street",
        "BICYCLE - 5 / 10.0 - DE street"
      )
    );
  }

  // A cost should be applied when switching to walking only.
  @Test
  public void testEndingWalkBike() {
    assertPath(
      A,
      F,
      StreetMode.BIKE,
      List.of(
        "null - 0 / 0.0 - null",
        "🚲WALK - 20 / 100.0 - AB street",
        "🚲WALK - 20 / 100.0 - BC street",
        "BICYCLE - 105 / 1010.0 - CD street",
        "BICYCLE - 5 / 10.0 - DE street",
        "BICYCLE - 5 / 10.0 - EF street"
      ),
      List.of(
        "null - 0 / 0.0 - null",
        "🚲WALK - 20 / 100.0 - AB street",
        "🚲WALK - 120 / 1100.0 - BC street",
        "BICYCLE - 5 / 10.0 - CD street",
        "BICYCLE - 5 / 10.0 - DE street",
        "BICYCLE - 5 / 10.0 - EF street"
      )
    );
  }

  // No cost should be applied, since no dismounting/mounting takes place.
  @Test
  public void testOnlyWalkBike() {
    assertBikePath(
      A,
      C,
      "null - 0 / 0.0 - null",
      "🚲WALK - 20 / 100.0 - AB street",
      "🚲WALK - 20 / 100.0 - BC street"
    );
  }

  // A cost should not be applied when entering / leaving since no mount / dismount takes place.
  @Test
  public void testEntranceStopLinkWalking() {
    assertWalkPath(
      S1,
      B,
      "null - 0 / 0.0 - null",
      "null - 0 / 1.0 - S1",
      "WALK - 10 / 20.0 - AB street"
    );

    assertWalkPath(
      E,
      E1,
      "null - 0 / 0.0 - null",
      "WALK - 10 / 20.0 - EF street",
      "null - 0 / 1.0 - E1"
    );
  }

  // A cost should not be applied when entering / leaving since no mount / dismount takes place.
  @Test
  public void testEntranceStopLinkBiking() {
    AB.setPermission(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);

    assertBikePath(
      S1,
      B,
      "null - 0 / 0.0 - null",
      "null - 0 / 1.0 - S1",
      "BICYCLE - 5 / 10.0 - AB street"
    );

    assertBikePath(
      E,
      E1,
      "null - 0 / 0.0 - null",
      "BICYCLE - 5 / 10.0 - EF street",
      "null - 0 / 1.0 - E1"
    );
  }

  @Test
  public void testEntranceStopLinkBikeWalking() {
    EF.setPermission(StreetTraversalPermission.PEDESTRIAN);

    assertBikePath(
      S1,
      B,
      "null - 0 / 0.0 - null",
      "null - 0 / 1.0 - S1",
      "🚲WALK - 20 / 100.0 - AB street"
    );

    assertBikePath(
      E,
      E1,
      "null - 0 / 0.0 - null",
      "🚲WALK - 20 / 100.0 - EF street",
      "null - 0 / 1.0 - E1"
    );
  }

  // A cost should be applied, since a mounting / dismounting takes place when entering the street network.
  @Test
  public void testPathwayBiking() {
    assertPath(
      E,
      S2,
      StreetMode.BIKE,
      List.of(
        "null - 0 / 0.0 - null",
        "BICYCLE - 5 / 10.0 - EF street",
        "null - 0 / 1.0 - E1",
        "🚲WALK - 160 / 1300.0 - E1S2 pathway"
      ),
      List.of(
        "null - 0 / 0.0 - null",
        "BICYCLE - 105 / 1010.0 - EF street",
        "null - 0 / 1.0 - E1",
        "🚲WALK - 60 / 300.0 - E1S2 pathway"
      )
    );
  }

  @Test
  public void testPathwayBikeWalking() {
    EF.setPermission(StreetTraversalPermission.PEDESTRIAN);

    assertBikePath(
      E,
      S2,
      "null - 0 / 0.0 - null",
      "🚲WALK - 20 / 100.0 - EF street",
      "null - 0 / 1.0 - E1",
      "🚲WALK - 60 / 300.0 - E1S2 pathway"
    );
  }

  @Test
  public void testPathwayWalking() {
    assertWalkPath(
      E,
      S2,
      "null - 0 / 0.0 - null",
      "WALK - 10 / 20.0 - EF street",
      "null - 0 / 1.0 - E1",
      "WALK - 60 / 120.0 - E1S2 pathway"
    );
  }

  // A cost should be applied when dismounting bike only.
  @Test
  public void testElevatorWalking() {
    assertWalkPath(
      C,
      Q,
      "null - 0 / 0.0 - null",
      "WALK - 10 / 20.0 - CD street",
      "null - 0 / 1.0 - null",
      "WALK - 90 / 90.0 - Elevator",
      "WALK - 20 / 20.0 - null",
      "WALK - 0 / 1.0 - L-Q",
      "null - 0 / 1.0 - null"
    );
  }

  // No cost is added since no mounting / dismounting takes place
  @Test
  public void testElevatorBikeWalking() {
    CD.setPermission(StreetTraversalPermission.PEDESTRIAN);

    assertBikePath(
      C,
      Q,
      "null - 0 / 0.0 - null",
      "🚲WALK - 20 / 100.0 - CD street",
      "null - 0 / 1.0 - null",
      "🚲WALK - 90 / 90.0 - Elevator",
      "🚲WALK - 20 / 20.0 - null",
      "🚲WALK - 0 / 1.0 - L-Q",
      "null - 0 / 1.0 - null"
    );
  }

  @Test
  public void testElevatorBiking() {
    assertPath(
      C,
      Q,
      StreetMode.BIKE,
      List.of(
        "null - 0 / 0.0 - null",
        "BICYCLE - 5 / 10.0 - CD street",
        "null - 0 / 1.0 - null",
        "🚲WALK - 190 / 1090.0 - Elevator",
        "🚲WALK - 20 / 20.0 - null",
        "🚲WALK - 0 / 1.0 - L-Q",
        "null - 0 / 1.0 - null"
      ),
      List.of(
        "null - 0 / 0.0 - null",
        "BICYCLE - 105 / 1010.0 - CD street",
        "null - 0 / 1.0 - null",
        "🚲WALK - 90 / 90.0 - Elevator",
        "🚲WALK - 20 / 20.0 - null",
        "🚲WALK - 0 / 1.0 - L-Q",
        "null - 0 / 1.0 - null"
      )
    );
  }

  @BeforeEach
  protected void setUp() throws Exception {
    // Generate a very simple graph
    //
    //   TS1 <-> A <-> B <-> C <-> D <-> E <-> F <-> E1 <-> S2

    modelOf(
      new Builder() {
        @Override
        public void build() {
          S1 = stop("S1", 0, 45);
          S2 = stop("S2", 0.005, 45);
          E1 = entrance("E1", 0.004, 45);
          A = intersection("A", 0.001, 45);
          B = intersection("B", 0.002, 45);
          C = intersection("C", 0.003, 45);
          D = intersection("D", 0.004, 45);
          E = intersection("E", 0.005, 45);
          F = intersection("F", 0.006, 45);
          Q = intersection("Q", 0.009, 45);

          elevator(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, D, Q);

          biLink(A, S1);
          AB = street(A, B, 100, StreetTraversalPermission.PEDESTRIAN, 40);
          BC = street(B, C, 100, StreetTraversalPermission.PEDESTRIAN, 40);
          CD = street(C, D, 100, StreetTraversalPermission.ALL, 40);
          DE = street(D, E, 100, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 40);
          EF = street(E, F, 100, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, 40);
          biLink(F, E1);
          pathway(E1, S2, 60, 100);
        }
      }
    );
  }

  private void assertBikePath(Vertex fromVertex, Vertex toVertex, String... descriptor) {
    assertPath(fromVertex, toVertex, StreetMode.BIKE, List.of(descriptor), List.of(descriptor));
  }

  private void assertWalkPath(Vertex fromVertex, Vertex toVertex, String... descriptor) {
    assertPath(fromVertex, toVertex, StreetMode.WALK, List.of(descriptor), List.of(descriptor));
  }

  private void assertPath(
    Vertex fromVertex,
    Vertex toVertex,
    StreetMode streetMode,
    List<String> expectedDepartAt,
    List<String> expectedArriveBy
  ) {
    List<String> departAt = runStreetSearchAndCreateDescriptor(
      fromVertex,
      toVertex,
      streetMode,
      false
    );
    List<String> arriveBy = runStreetSearchAndCreateDescriptor(
      fromVertex,
      toVertex,
      streetMode,
      true
    );

    assertEquals(expectedDepartAt, departAt, "departAt");
    assertEquals(expectedArriveBy, arriveBy, "arriveBy");
  }

  private List<String> runStreetSearchAndCreateDescriptor(
    Vertex fromVertex,
    Vertex toVertex,
    StreetMode streetMode,
    boolean arriveBy
  ) {
    var request = RouteRequest.of()
      .withPreferences(preferences ->
        preferences
          .withWalk(w -> w.withSpeed(10))
          .withBike(it ->
            it
              .withSpeed(20d)
              .withWalking(w ->
                w.withSpeed(5d).withMountDismountTime(100).withMountDismountCost(1000)
              )
          )
      )
      .withArriveBy(arriveBy)
      .buildDefault();

    var tree = StreetSearchBuilder.of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(request)
      .setStreetRequest(new StreetRequest(streetMode))
      .setFrom(fromVertex)
      .setTo(toVertex)
      .getShortestPathTree();

    var path = tree.getPath(arriveBy ? fromVertex : toVertex);

    if (path == null) {
      return null;
    }

    return path.states
      .stream()
      .map(s ->
        String.format(
          "%s%s - %s / %s - %s",
          s.getBackMode() != null && s.isBackWalkingBike() ? "🚲" : "",
          s.getBackMode(),
          s.getTimeDeltaSeconds(),
          s.getBackEdge() != null ? ((double) Math.round(s.getWeightDelta() * 10)) / 10 : 0.0,
          s.getBackEdge() != null ? s.getBackEdge().getDefaultName() : null
        )
      )
      .collect(Collectors.toList());
  }
}
