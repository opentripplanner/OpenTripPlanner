package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TripTimesForDaysIndex.applyOffsets;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TripTimesForDaysIndexTest {

  @Test
  void testHappyDayAccessors() {
    int[] offsets = { 8, 16 };
    var departureTimesPerDay = List.of(new int[] { 10, 30 }, new int[] { 20, 40 });
    var subject = TripTimesForDaysIndex.ofTripTimesForDay(departureTimesPerDay, offsets);

    assertEquals(0, subject.day(0));
    assertEquals(1, subject.day(1));
    assertEquals(0, subject.day(2));
    assertEquals(1, subject.day(3));

    assertEquals(0, subject.tripIndexForDay(0));
    assertEquals(0, subject.tripIndexForDay(1));
    assertEquals(1, subject.tripIndexForDay(2));
    assertEquals(1, subject.tripIndexForDay(3));

    assertEquals(4, subject.size());
  }

  /**
   * Return a list of test-cases with the input and the expected result, both as Strings.
   *  - input format:  1 2 | 3 4  Trips times, where each day is separated by a '|'.
   *  - expected format: (<day>:<tripIndex for day>) for each departure.
   *
   *  Note!
   *    A case like "1 | 1 | 1" is not valid, only two following days may overlap in time, and
   *    the behaviour of {@link TripTimesForDaysIndex} in such cases is undefined.
   */
  static List<Arguments> initializationTestCases() {
    return Arrays.stream(
      """
      # Test with one day
      1              ->  0:0
      1 2            ->  0:0 0:1
      1 1            ->  0:0 0:1

      # Test with two days
      1 | 2          ->  0:0 1:0
      2 | 1          ->  1:0 0:0
      1 | 1          ->  0:0 1:0
      1 2 3          ->  0:0 0:1 0:2
      1 2 | 3        ->  0:0 0:1 1:0
      1 3 | 2        ->  0:0 1:0 0:1
      2 3 | 1        ->  1:0 0:0 0:1
      1 | 2 3        ->  0:0 1:0 1:1
      2 | 1 3        ->  1:0 0:0 1:1
      3 | 1 2        ->  1:0 1:1 0:0
      1 | 2 | 3      ->  0:0 1:0 2:0
      2 | 1 | 3      ->  1:0 0:0 2:0
      1 | 3 | 2      ->  0:0 2:0 1:0
      1 | 2 | 2      ->  0:0 1:0 2:0
      1 | 1 | 2      ->  0:0 1:0 2:0

      # Test all overlapping times with 3 days and 5 times
      1 2 3 | 4 | 5  ->  0:0 0:1 0:2 1:0 2:0
      1 2 3 | 5 | 4  ->  0:0 0:1 0:2 2:0 1:0
      1 2 | 3 4 | 5  ->  0:0 0:1 1:0 1:1 2:0
      1 2 | 3 5 | 4  ->  0:0 0:1 1:0 2:0 1:1
      1 2 | 4 5 | 3  ->  0:0 0:1 2:0 1:0 1:1
      1 3 | 2 4 | 5  ->  0:0 1:0 0:1 1:1 2:0
      1 3 | 2 5 | 4  ->  0:0 1:0 0:1 2:0 1:1
      1 4 | 2 3 | 5  ->  0:0 1:0 1:1 0:1 2:0
      2 3 | 1 4 | 5  ->  1:0 0:0 0:1 1:1 2:0
      2 3 | 1 5 | 4  ->  1:0 0:0 0:1 2:0 1:1
      1 2 | 3 | 4 5  ->  0:0 0:1 1:0 2:0 2:1
      1 2 | 4 | 3 5  ->  0:0 0:1 2:0 1:0 2:1
      1 2 | 5 | 3 4  ->  0:0 0:1 2:0 2:1 1:0
      1 3 | 2 | 4 5  ->  0:0 1:0 0:1 2:0 2:1
      2 3 | 1 | 4 5  ->  1:0 0:0 0:1 2:0 2:1
      1 | 2 3 4 | 5  ->  0:0 1:0 1:1 1:2 2:0
      1 | 2 3 5 | 4  ->  0:0 1:0 1:1 2:0 1:2
      1 | 2 4 5 | 3  ->  0:0 1:0 2:0 1:1 1:2
      1 | 3 4 5 | 2  ->  0:0 2:0 1:0 1:1 1:2
      2 | 1 3 4 | 5  ->  1:0 0:0 1:1 1:2 2:0
      2 | 1 3 5 | 4  ->  1:0 0:0 1:1 2:0 1:2
      2 | 1 4 5 | 3  ->  1:0 0:0 2:0 1:1 1:2
      3 | 1 2 4 | 5  ->  1:0 1:1 0:0 1:2 2:0
      3 | 1 2 5 | 4  ->  1:0 1:1 0:0 2:0 1:2
      4 | 1 2 3 | 5  ->  1:0 1:1 1:2 0:0 2:0
      1 | 2 3 | 4 5  ->  0:0 1:0 1:1 2:0 2:1
      1 | 2 4 | 3 5  ->  0:0 1:0 2:0 1:1 2:1
      2 | 1 4 | 3 5  ->  1:0 0:0 2:0 1:1 2:1
      1 | 2 5 | 3 4  ->  0:0 1:0 2:0 2:1 1:1
      2 | 1 5 | 3 4  ->  1:0 0:0 2:0 2:1 1:1
      1 | 3 4 | 2 5  ->  0:0 2:0 1:0 1:1 2:1
      1 | 3 5 | 2 4  ->  0:0 2:0 1:0 2:1 1:1
      1 | 4 5 | 2 3  ->  0:0 2:0 2:1 1:0 1:1
      1 | 2 | 3 4 5  ->  0:0 1:0 2:0 2:1 2:2
      2 | 1 | 3 4 5  ->  1:0 0:0 2:0 2:1 2:2
      1 | 3 | 2 4 5  ->  0:0 2:0 1:0 2:1 2:2
      1 | 4 | 2 3 5  ->  0:0 2:0 2:1 1:0 2:2
      1 | 5 | 2 3 4  ->  0:0 2:0 2:1 2:2 1:0
      """.split("\n")
    )
      .map(String::trim)
      .filter(s -> s.length() > 0)
      .filter(s -> !s.startsWith("#"))
      .map(s -> Arguments.of(s.substring(0, 14).trim(), s.substring(18).trim()))
      .toList();
  }

  @ParameterizedTest
  @MethodSource("initializationTestCases")
  void testIniitalization(String input, String expected) {
    var subject = new TripTimesForDaysIndex(toTimes(input));
    assertEquals(expected, subject.toString());
  }

  static List<Arguments> applyOffsetsTestCases() {
    return List.of(
      Arguments.of("[11]", "1", new int[] { 10 }),
      Arguments.of("[11] [22]", "1 | 2", new int[] { 10, 20 }),
      Arguments.of("[11, 12] [23]", "1 2 | 3", new int[] { 10, 20 }),
      Arguments.of("[11, 111] [29]", "1 101 | 9", new int[] { 10, 20 })
    );
  }

  @ParameterizedTest
  @MethodSource("applyOffsetsTestCases")
  void testApplyOffsets(String expected, String input, int[] offsets) {
    assertEquals(expected, toString(applyOffsets(toTimes(input), offsets)));
  }

  private String toString(List<int[]> times) {
    return times.stream().map(Arrays::toString).collect(Collectors.joining(" "));
  }

  static List<int[]> toTimes(String list) {
    var args = list.split("\\|");
    return Arrays.stream(args)
      .map(it -> Arrays.stream(it.trim().split(" ")).mapToInt(Integer::parseInt).toArray())
      .toList();
  }
}
