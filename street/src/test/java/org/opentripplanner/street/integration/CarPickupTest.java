package org.opentripplanner.street.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitEntranceVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;

/**
 * Test CarPickup: - it may start with (WALK - WALK_TO_PICKUP, CAR - IN_CAR) - it may end with (WALK
 * - WALK_FROM_DROP_OFF, CAR - IN_CAR) - StreetTransitEntityLink require mode changes to WALK -
 * StreetEdges may contain mode changes between CAR / WALK
 * <p>
 * arriveBy and departAt paths should be symmetric.
 * <p>
 * Parametrized over {@code CAR_PICKUP} and {@code TAXI}, since both share the identical
 * walk-drive-walk state machine.
 */
public class CarPickupTest extends GraphRoutingTest {

  private TransitStopVertex S1;
  private TransitEntranceVertex E1;
  private StreetVertex A;
  private StreetVertex B;
  private StreetVertex C;
  private StreetVertex D;
  private StreetVertex E;

  @ParameterizedTest
  @EnumSource(value = StreetMode.class, names = { "CAR_PICKUP", "TAXI" })
  public void testCarPickupCarOnly(StreetMode mode) {
    assertPath(mode, B, C, "null - IN_CAR - null, CAR - IN_CAR - BC street");
  }

  @ParameterizedTest
  @EnumSource(value = StreetMode.class, names = { "CAR_PICKUP", "TAXI" })
  public void testCarPickupCarThenWalk(StreetMode mode) {
    assertPath(
      mode,
      A,
      C,
      "null - WALK_TO_PICKUP - null, WALK - WALK_TO_PICKUP - AB street, CAR - IN_CAR - BC street"
    );
  }

  @ParameterizedTest
  @EnumSource(value = StreetMode.class, names = { "CAR_PICKUP", "TAXI" })
  public void testCarPickupFromEntranceThenCar(StreetMode mode) {
    assertPath(
      mode,
      S1,
      C,
      "null - WALK_TO_PICKUP - null, null - WALK_TO_PICKUP - S1, CAR - IN_CAR - BC street"
    );
  }

  @ParameterizedTest
  @EnumSource(value = StreetMode.class, names = { "CAR_PICKUP", "TAXI" })
  public void testCarPickupWalkFromEntranceThenCarThenWalk(StreetMode mode) {
    assertPath(
      mode,
      S1,
      D,
      "null - WALK_TO_PICKUP - null, null - WALK_TO_PICKUP - S1, CAR - IN_CAR - BC street, WALK - WALK_FROM_DROP_OFF - CD street"
    );
  }

  @ParameterizedTest
  @EnumSource(value = StreetMode.class, names = { "CAR_PICKUP", "TAXI" })
  public void testCarPickupCarThenWalkToStop(StreetMode mode) {
    assertPath(
      mode,
      B,
      E1,
      "null - IN_CAR - null, CAR - IN_CAR - BC street, null - WALK_FROM_DROP_OFF - E1"
    );
  }

  @ParameterizedTest
  @EnumSource(value = StreetMode.class, names = { "CAR_PICKUP", "TAXI" })
  public void testCarPickupWalkFromEntranceThenCarThenWalkToStop(StreetMode mode) {
    assertPath(
      mode,
      S1,
      E1,
      "null - WALK_TO_PICKUP - null, null - WALK_TO_PICKUP - S1, CAR - IN_CAR - BC street, null - WALK_FROM_DROP_OFF - E1"
    );
  }

  @ParameterizedTest
  @EnumSource(value = StreetMode.class, names = { "CAR_PICKUP", "TAXI" })
  public void testCarPickupWalkThenCarThenWalk(StreetMode mode) {
    assertPath(
      mode,
      A,
      D,
      "null - WALK_TO_PICKUP - null, WALK - WALK_TO_PICKUP - AB street, CAR - IN_CAR - BC street, WALK - WALK_FROM_DROP_OFF - CD street"
    );
  }

  @ParameterizedTest
  @EnumSource(value = StreetMode.class, names = { "CAR_PICKUP", "TAXI" })
  public void testWalkOnlyCarPickup(StreetMode mode) {
    // This is a special case where the reverse states differ, due to both starting in the IN_CAR
    // state and switching to walking when encountering the first edge. This is the only valid
    // path since a CarPickup must be in `IN_CAR` or `WALK_FROM_DROP_OFF` to be a final state,
    // and the path can't be traversed by car.
    assertPath(
      mode,
      A,
      B,
      "null - WALK_TO_PICKUP - null, WALK - WALK_TO_PICKUP - AB street",
      "null - WALK_FROM_DROP_OFF - null, WALK - WALK_FROM_DROP_OFF - AB street"
    );
  }

  @BeforeEach
  protected void setUp() throws Exception {
    // Generate a very simple graph
    //
    //   A <-> B <-> C <-> D <-> E
    //   TS1 <-^           ^-> TE1

    modelOf(
      new Builder() {
        @Override
        public void build() {
          S1 = stop("S1", 0, 45);
          E1 = entrance("E1", 0.004, 45);
          A = intersection("A", 0.001, 45);
          B = intersection("B", 0.002, 45);
          C = intersection("C", 0.003, 45);
          D = intersection("D", 0.004, 45);
          E = intersection("E", 0.005, 45);

          biLink(B, S1);
          biLink(C, E1);

          street(A, B, 87, StreetTraversalPermission.PEDESTRIAN);
          street(B, C, 87, StreetTraversalPermission.CAR);
          street(C, D, 87, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
          street(D, E, 87, StreetTraversalPermission.PEDESTRIAN);
        }
      }
    );
  }

  private void assertPath(StreetMode mode, Vertex fromVertex, Vertex toVertex, String descriptor) {
    String departAt = runStreetSearchAndCreateDescriptor(mode, fromVertex, toVertex, false);
    String arriveBy = runStreetSearchAndCreateDescriptor(mode, fromVertex, toVertex, true);

    assertDescriptors(descriptor, descriptor, arriveBy, departAt);
  }

  private void assertPath(
    StreetMode mode,
    Vertex fromVertex,
    Vertex toVertex,
    String expectedDepartAt,
    String expectedArriveBy
  ) {
    String departAt = runStreetSearchAndCreateDescriptor(mode, fromVertex, toVertex, false);
    String arriveBy = runStreetSearchAndCreateDescriptor(mode, fromVertex, toVertex, true);

    assertDescriptors(expectedDepartAt, expectedArriveBy, arriveBy, departAt);
  }

  private void assertDescriptors(
    String expectedDepartAt,
    String expectedArriveBy,
    String arriveBy,
    String departAt
  ) {
    String formatString = "DepartAt: %s%nArriveBy: %s";

    assertEquals(
      String.format(formatString, expectedDepartAt, expectedArriveBy),
      String.format(formatString, departAt, arriveBy)
    );
  }

  private String runStreetSearchAndCreateDescriptor(
    StreetMode mode,
    Vertex fromVertex,
    Vertex toVertex,
    boolean arriveBy
  ) {
    var request = StreetSearchRequest.of().withMode(mode).withArriveBy(arriveBy).build();

    var tree = StreetSearchBuilder.of()
      .withHeuristic(new EuclideanRemainingWeightHeuristic())
      .withRequest(request)
      .withFrom(fromVertex)
      .withTo(toVertex)
      .getShortestPathTree();
    var path = tree.getPath(arriveBy ? fromVertex : toVertex);

    return path != null
      ? path.states
          .stream()
          .map(s ->
            String.format(
              "%s - %s - %s",
              s.getBackMode(),
              s.getCarPickupState(),
              s.getBackEdge() != null ? s.getBackEdge().getDefaultName() : null
            )
          )
          .collect(Collectors.joining(", "))
      : "path not found";
  }
}
