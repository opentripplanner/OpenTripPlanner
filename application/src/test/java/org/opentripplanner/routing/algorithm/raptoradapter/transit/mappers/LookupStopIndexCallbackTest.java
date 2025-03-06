package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.EntityNotFoundException;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class LookupStopIndexCallbackTest {

  private static final FeedScopedId ID_1 = FeedScopedId.ofNullable("F", "1");
  private static final FeedScopedId ID_2 = FeedScopedId.ofNullable("F", "2");
  private static final FeedScopedId ID_3 = FeedScopedId.ofNullable("F", "3");

  private final LookupStopIndexCallback subject = new TestLookupStopIndexCallback(
    Map.of(ID_1, new int[] { 1, 7, 13 }, ID_2, new int[] { 2, 7, 15 })
  );

  /**
   * This mostly verifies that the test is set up correctly, the code tested is the dummy inside
   * the test.
   */
  void lookupStopLocationIndexesSingleIdInput() {
    assertArrayEquals(new int[] { 1, 7, 13 }, subject.lookupStopLocationIndexes(ID_1).toArray());
    var ex = Assertions.assertThrows(EntityNotFoundException.class, () ->
      subject.lookupStopLocationIndexes(ID_3).toArray()
    );
    assertEquals("StopLocation does not exist for id F:3", ex.getMessage());
  }

  @Test
  void lookupStopLocationIndexesCollectionInput() {
    assertArrayEquals(new int[] {}, subject.lookupStopLocationIndexes(List.of()));
    assertArrayEquals(new int[] { 1, 7, 13 }, subject.lookupStopLocationIndexes(List.of(ID_1)));
    assertArrayEquals(
      new int[] { 1, 2, 7, 13, 15 },
      subject.lookupStopLocationIndexes(List.of(ID_1, ID_2))
    );

    // Should throw exception?
    var ex = assertThrows(EntityNotFoundException.class, () ->
      subject.lookupStopLocationIndexes(List.of(ID_1, ID_3))
    );
    assertEquals("StopLocation entity not found: F:3", ex.getMessage());
  }
}
