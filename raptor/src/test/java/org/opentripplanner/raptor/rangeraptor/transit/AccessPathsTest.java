package org.opentripplanner.raptor.rangeraptor.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flex;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flexAndWalk;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;
import static org.opentripplanner.raptor.api.model.SearchDirection.FORWARD;
import static org.opentripplanner.raptor.api.model.SearchDirection.REVERSE;
import static org.opentripplanner.raptor.api.request.RaptorProfile.MULTI_CRITERIA;
import static org.opentripplanner.raptor.api.request.RaptorProfile.STANDARD;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.request.RaptorProfile;

class AccessPathsTest implements RaptorTestConstants {

  // Walking paths
  public static final TestAccessEgress WALK_FAST = walk(STOP_A, 29, 900);
  public static final TestAccessEgress WALK_MEDIUM = walk(STOP_A, 30, 800);
  public static final TestAccessEgress WALK_COST = walk(STOP_A, 31, 700);
  public static final TestAccessEgress WALK_BAD = walk(STOP_A, 30, 900);
  public static final TestAccessEgress WALK_B = walk(STOP_B, 60, 2000);

  // Flex with on board arrival
  public static final TestAccessEgress FLEX_FAST = flex(STOP_A, 25, 3, 900);
  public static final TestAccessEgress FLEX_COST = flex(STOP_A, 31, 3, 500);
  public static final TestAccessEgress FLEX_TX2 = flex(STOP_A, 26, 2, 900);
  public static final TestAccessEgress FLEX_BAD = flex(STOP_A, 26, 3, 900);
  public static final TestAccessEgress FLEX_B = flex(STOP_B, 50, 3, 2000);

  // Flex with walking
  public static final TestAccessEgress FLEX_WALK_FAST = flexAndWalk(STOP_A, 25, 2, 900);
  public static final TestAccessEgress FLEX_WALK_COST = flexAndWalk(STOP_A, 31, 2, 400);
  public static final TestAccessEgress FLEX_WALK_TX1 = flexAndWalk(STOP_A, 28, 1, 900);
  public static final TestAccessEgress FLEX_WALK_BAD = flexAndWalk(STOP_A, 26, 2, 900);
  public static final TestAccessEgress FLEX_WALK_B = flexAndWalk(STOP_B, 50, 2, 2000);

  @Test
  void arrivedOnStreetByNumOfRides() {
    var accessPaths = create(STANDARD);
    expect(accessPaths.arrivedOnStreetByNumOfRides(0), WALK_FAST, WALK_B);
    expect(accessPaths.arrivedOnStreetByNumOfRides(1), FLEX_WALK_TX1);
    expect(accessPaths.arrivedOnStreetByNumOfRides(2), FLEX_WALK_FAST, FLEX_WALK_B);
    expect(accessPaths.arrivedOnStreetByNumOfRides(3));

    accessPaths = create(MULTI_CRITERIA);
    expect(accessPaths.arrivedOnStreetByNumOfRides(0), WALK_FAST, WALK_MEDIUM, WALK_COST, WALK_B);
    expect(accessPaths.arrivedOnStreetByNumOfRides(1), FLEX_WALK_TX1);
    expect(accessPaths.arrivedOnStreetByNumOfRides(2), FLEX_WALK_FAST, FLEX_WALK_COST, FLEX_WALK_B);
    expect(accessPaths.arrivedOnStreetByNumOfRides(3));
  }

  @Test
  void arrivedOnBoardByNumOfRides() {
    var accessPaths = create(STANDARD);
    expect(accessPaths.arrivedOnBoardByNumOfRides(0));
    expect(accessPaths.arrivedOnBoardByNumOfRides(1));
    expect(accessPaths.arrivedOnBoardByNumOfRides(2), FLEX_TX2);
    expect(accessPaths.arrivedOnBoardByNumOfRides(3), FLEX_FAST, FLEX_B);
    expect(accessPaths.arrivedOnBoardByNumOfRides(4));

    accessPaths = create(MULTI_CRITERIA);
    expect(accessPaths.arrivedOnBoardByNumOfRides(0));
    expect(accessPaths.arrivedOnBoardByNumOfRides(1));
    expect(accessPaths.arrivedOnBoardByNumOfRides(2), FLEX_TX2);
    expect(accessPaths.arrivedOnBoardByNumOfRides(3), FLEX_FAST, FLEX_COST, FLEX_B);
    expect(accessPaths.arrivedOnBoardByNumOfRides(4));
  }

  @Test
  void calculateMaxNumberOfRides() {
    assertEquals(3, create(STANDARD).calculateMaxNumberOfRides());
    assertEquals(3, create(MULTI_CRITERIA).calculateMaxNumberOfRides());
  }

  @Test
  void iterateOverPathsWithTimePenalty() {
    // Expected at departure 540
    var flexFastWithPenalty = FLEX_FAST.withTimePenalty(60);

    // Expected at departure 540 and 480
    var flexTxWithPenalty = FLEX_TX2.withTimePenalty(61);
    var flexCostWithPenalty = FLEX_COST.withTimePenalty(120);

    // Expected at departure 540, 480 and 420
    var walkFastWithPenalty = WALK_FAST.withTimePenalty(121);
    var walkCostWithPenalty = WALK_COST.withTimePenalty(180);

    // Without time-penalty, the iterator should be empty
    var accessPaths = AccessPaths.create(
      60,
      List.of(
        flexFastWithPenalty,
        flexTxWithPenalty,
        flexCostWithPenalty,
        walkFastWithPenalty,
        walkCostWithPenalty,
        // Should be filtered away
        WALK_B,
        FLEX_B,
        FLEX_WALK_B
      ),
      MULTI_CRITERIA,
      FORWARD
    );

    // Make sure standard iterator works
    expect(
      accessPaths.arrivedOnStreetByNumOfRides(0),
      WALK_B,
      walkFastWithPenalty,
      walkCostWithPenalty
    );
    expect(accessPaths.arrivedOnBoardByNumOfRides(1));
    expect(accessPaths.arrivedOnStreetByNumOfRides(1));
    expect(accessPaths.arrivedOnBoardByNumOfRides(2), flexTxWithPenalty);
    expect(accessPaths.arrivedOnStreetByNumOfRides(2), FLEX_WALK_B);
    expect(
      accessPaths.arrivedOnBoardByNumOfRides(3),
      FLEX_B,
      flexFastWithPenalty,
      flexCostWithPenalty
    );
    expect(accessPaths.arrivedOnStreetByNumOfRides(3));

    var iterator = accessPaths.iterateOverPathsWithPenalty(600);

    // First iteration
    assertTrue(iterator.hasNext());
    assertEquals(540, iterator.next());
    expect(accessPaths.arrivedOnStreetByNumOfRides(0), walkFastWithPenalty, walkCostWithPenalty);
    expect(accessPaths.arrivedOnBoardByNumOfRides(1));
    expect(accessPaths.arrivedOnStreetByNumOfRides(1));
    expect(accessPaths.arrivedOnBoardByNumOfRides(2), flexTxWithPenalty);
    expect(accessPaths.arrivedOnStreetByNumOfRides(2));
    //expect(accessPaths.arrivedOnBoardByNumOfRides(3), flexFastWithPenalty, flexCostWithPenalty);
    expect(accessPaths.arrivedOnStreetByNumOfRides(3));
    expect(accessPaths.arrivedOnBoardByNumOfRides(4));
    expect(accessPaths.arrivedOnStreetByNumOfRides(4));

    // Second iteration
    assertTrue(iterator.hasNext());
    assertEquals(480, iterator.next());
    expect(accessPaths.arrivedOnStreetByNumOfRides(0), walkFastWithPenalty, walkCostWithPenalty);
    expect(accessPaths.arrivedOnBoardByNumOfRides(2), flexTxWithPenalty);
    expect(accessPaths.arrivedOnBoardByNumOfRides(3), flexCostWithPenalty);

    // Third iteration
    assertTrue(iterator.hasNext());
    assertEquals(420, iterator.next());
    expect(accessPaths.arrivedOnStreetByNumOfRides(0), walkFastWithPenalty, walkCostWithPenalty);
    expect(accessPaths.arrivedOnBoardByNumOfRides(2));
    expect(accessPaths.arrivedOnBoardByNumOfRides(3));

    assertFalse(iterator.hasNext());
  }

  @Test
  void iterateOverPathsWithTimePenaltyInReversDirection() {
    // Expected at departure 540
    var flexFastWithPenalty = FLEX_FAST.withTimePenalty(60);

    // Expected at departure 540 and 480
    var flexTxWithPenalty = FLEX_TX2.withTimePenalty(61);

    // Expected at departure 540, 480 and 420
    var walkFastWithPenalty = WALK_FAST.withTimePenalty(121);

    // Without time-penalty, the iterator should be empty
    var accessPaths = AccessPaths.create(
      60,
      List.of(flexFastWithPenalty, flexTxWithPenalty, walkFastWithPenalty, WALK_B, FLEX_B),
      STANDARD,
      REVERSE
    );

    // Make sure standard iterator works
    expect(accessPaths.arrivedOnStreetByNumOfRides(0), WALK_B, walkFastWithPenalty);
    expect(accessPaths.arrivedOnBoardByNumOfRides(3), FLEX_B, flexFastWithPenalty);

    var iterator = accessPaths.iterateOverPathsWithPenalty(600);

    // First iteration
    assertTrue(iterator.hasNext());
    assertEquals(660, iterator.next());
    expect(accessPaths.arrivedOnStreetByNumOfRides(0), walkFastWithPenalty);
    expect(accessPaths.arrivedOnBoardByNumOfRides(1));
    expect(accessPaths.arrivedOnStreetByNumOfRides(1));
    expect(accessPaths.arrivedOnBoardByNumOfRides(2), flexTxWithPenalty);
    expect(accessPaths.arrivedOnStreetByNumOfRides(2));
    expect(accessPaths.arrivedOnBoardByNumOfRides(3), flexFastWithPenalty);
    expect(accessPaths.arrivedOnStreetByNumOfRides(3));
    expect(accessPaths.arrivedOnBoardByNumOfRides(4));
    expect(accessPaths.arrivedOnStreetByNumOfRides(4));

    // Second iteration
    assertTrue(iterator.hasNext());
    assertEquals(720, iterator.next());
    expect(accessPaths.arrivedOnStreetByNumOfRides(0), walkFastWithPenalty);
    expect(accessPaths.arrivedOnBoardByNumOfRides(2), flexTxWithPenalty);
    expect(accessPaths.arrivedOnBoardByNumOfRides(3));

    // Third iteration
    assertTrue(iterator.hasNext());
    assertEquals(780, iterator.next());
    expect(accessPaths.arrivedOnStreetByNumOfRides(0), walkFastWithPenalty);
    expect(accessPaths.arrivedOnBoardByNumOfRides(2));
    expect(accessPaths.arrivedOnBoardByNumOfRides(3));

    assertFalse(iterator.hasNext());
  }

  @Test
  void testRegularIteratorsAndIteratorWithPenaltyWorksTogether() {
    var walkFastWithPenalty = WALK_FAST.withTimePenalty(60);

    // Without time-penalty, the iterator should be empty
    var accessPaths = AccessPaths.create(
      60,
      List.of(walkFastWithPenalty, WALK_COST),
      MULTI_CRITERIA,
      FORWARD
    );

    // Both accesses are expected before with enter the "time-penalty" iteration
    expect(accessPaths.arrivedOnStreetByNumOfRides(0), WALK_COST, walkFastWithPenalty);
    expect(accessPaths.arrivedOnBoardByNumOfRides(0));

    var iterator = accessPaths.iterateOverPathsWithPenalty(600);

    // First iteration - only access with time-penalty is expected
    assertTrue(iterator.hasNext());
    assertEquals(540, iterator.next());
    expect(accessPaths.arrivedOnStreetByNumOfRides(0), walkFastWithPenalty);
    expect(accessPaths.arrivedOnBoardByNumOfRides(0));

    // Second iteration - Done
    assertFalse(iterator.hasNext());
  }

  @Test
  void hasTimeDependentAccess() {
    var accessPaths = AccessPaths.create(
      60,
      List.of(WALK_FAST, walk(STOP_A, 20).openingHours(1200, 2400)),
      STANDARD,
      FORWARD
    );
    assertTrue(accessPaths.hasTimeDependentAccess(), "Time dependent access is better.");

    accessPaths = AccessPaths.create(
      60,
      List.of(WALK_FAST, walk(STOP_A, 50).openingHours(1200, 2400)),
      STANDARD,
      REVERSE
    );
    assertFalse(accessPaths.hasTimeDependentAccess(), "Time dependent access is worse.");
  }

  @Test
  void hasNoTimeDependentAccess() {
    var accessPaths = create(STANDARD);
    assertFalse(accessPaths.hasTimeDependentAccess());

    var it = accessPaths.iterateOverPathsWithPenalty(600);
    assertFalse(it.hasNext());
  }

  private static void expect(
    Collection<RaptorAccessEgress> result,
    RaptorAccessEgress... expected
  ) {
    var r = result.stream().map(Objects::toString).sorted().toList();
    var e = Arrays.stream(expected).map(Objects::toString).sorted().toList();
    assertEquals(e, r);
  }

  private static AccessPaths create(RaptorProfile profile) {
    Collection<RaptorAccessEgress> accessPaths = List.of(
      WALK_FAST,
      WALK_MEDIUM,
      WALK_COST,
      WALK_BAD,
      WALK_B,
      FLEX_WALK_FAST,
      FLEX_WALK_COST,
      FLEX_WALK_TX1,
      FLEX_WALK_BAD,
      FLEX_WALK_B,
      FLEX_FAST,
      FLEX_COST,
      FLEX_TX2,
      FLEX_BAD,
      FLEX_B
    );
    return AccessPaths.create(60, accessPaths, profile, FORWARD);
  }
}
