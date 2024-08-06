package org.opentripplanner.raptor.rangeraptor.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flex;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flexAndWalk;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.request.RaptorProfile;

class EgressPathsTest {

  private static final int STOP_A = 2;
  private static final int STOP_B = 3;
  private static final int STOP_C = 4;
  private static final int STOP_D = 5;
  private static final int STOP_E = 6;
  private static final int[] ALL_STOPS = new int[] { STOP_A, STOP_B, STOP_C, STOP_D, STOP_E };

  private static final int D1m = DurationUtils.durationInSeconds("1m");
  private static final int D2m = DurationUtils.durationInSeconds("2m");

  private final List<RaptorAccessEgress> egresses = List.of(
    // Simple, one egress
    walk(STOP_A, D1m),
    // Duration, short is better
    flex(STOP_B, D1m),
    flex(STOP_B, D2m),
    // Reached on-board is better than on-street
    flex(STOP_C, D1m),
    flexAndWalk(STOP_C, D1m),
    // Number of rides, smallest is better
    flex(STOP_D, D1m, 2),
    flex(STOP_D, D1m, 3),
    // Opening Hours dominate each other (no check on overlapping)
    walk(STOP_E, D2m),
    walk(STOP_E, D1m).openingHours("10:00", "11:45"),
    walk(STOP_E, D1m).openingHours("11:30", "12:30"),
    walk(STOP_E, D2m).openingHours("14:00", "14:00")
  );

  private final EgressPaths subjectStd = EgressPaths.create(egresses, RaptorProfile.STANDARD);
  private final EgressPaths subjectMc = EgressPaths.create(egresses, RaptorProfile.MULTI_CRITERIA);

  @Test
  void byStopStandard() {
    var byStop = subjectStd.byStop();
    assertEquals("[Walk 1m C₁120 ~ 2]", byStop.get(STOP_A).toString());
    assertEquals("[Flex 1m C₁120 1x ~ 3]", byStop.get(STOP_B).toString());
    assertEquals("[Flex 1m C₁120 1x ~ 4]", byStop.get(STOP_C).toString());
    assertEquals("[Flex 1m C₁120 2x ~ 5]", byStop.get(STOP_D).toString());
    assertEquals(
      "[Walk 2m C₁240 ~ 6, Walk 1m C₁120 Open(10:00 11:45) ~ 6, Walk 1m C₁120 Open(11:30 12:30) ~ 6]",
      byStop.get(STOP_E).toString()
    );
    // Verify no more stops (A, B, C, D, E)
    assertEquals(toString(ALL_STOPS), toString(byStop.keys()));
  }

  @Test
  void stops() {
    assertEquals(toString(ALL_STOPS), toString(subjectStd.stops()));
    assertEquals(toString(ALL_STOPS), toString(subjectMc.stops()));
  }

  @Test
  void listAll() {
    assertEquals(
      """
      Flex 1m C₁120 1x ~ 3
      Flex 1m C₁120 1x ~ 4
      Flex 1m C₁120 2x ~ 5
      Walk 1m C₁120 Open(10:00 11:45) ~ 6
      Walk 1m C₁120 Open(11:30 12:30) ~ 6
      Walk 1m C₁120 ~ 2
      Walk 2m C₁240 ~ 6
      """.strip(),
      subjectStd.listAll().stream().map(Object::toString).sorted().collect(Collectors.joining("\n"))
    );

    assertEquals(
      """
      Flex 1m C₁120 1x ~ 3
      Flex 1m C₁120 1x ~ 4
      Flex 1m C₁120 2x ~ 5
      Walk 1m C₁120 Open(10:00 11:45) ~ 6
      Walk 1m C₁120 Open(11:30 12:30) ~ 6
      Walk 1m C₁120 ~ 2
      Walk 2m C₁240 ~ 6
      """.strip(),
      subjectMc.listAll().stream().map(Object::toString).sorted().collect(Collectors.joining("\n"))
    );
  }

  @Test
  void walkToDestinationEgressStops() {
    assertEquals(
      toString(new int[] { STOP_A, STOP_E }),
      toString(subjectStd.egressesWitchStartByWalking())
    );

    //[2, 6]
    assertEquals(
      toString(new int[] { STOP_A, STOP_E }),
      toString(subjectMc.egressesWitchStartByWalking())
    );
  }

  @Test
  void rideToDestinationEgressStops() {
    int[] expected = { STOP_B, STOP_C, STOP_D };
    assertEquals(toString(expected), toString(subjectStd.egressesWitchStartByARide()));
    assertEquals(toString(expected), toString(subjectMc.egressesWitchStartByARide()));
  }

  String toString(int[] ints) {
    return Arrays.toString(IntStream.of(ints).sorted().toArray());
  }
}
