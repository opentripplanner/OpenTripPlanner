package org.opentripplanner.transit.model.trip.timetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model.trip.Timetable.NEXT_TIME_TABLE_INDEX;
import static org.opentripplanner.transit.model.trip.Timetable.PREV_TIME_TABLE_INDEX;
import static org.opentripplanner.transit.model.trip.timetable.BoardTimeSearch.findBoardTime;
import static org.opentripplanner.transit.model.trip.timetable.BoardTimeSearch.findBoardTimeBinarySearch;
import static org.opentripplanner.transit.model.trip.timetable.BoardTimeSearch.findBoardTimeLinearApproximation;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.time.TimeUtils;

class BoardTimeSearchTest {

  private final int[] ONE = { 10 };
  private final int[] FOUR = { 10, 11, 11, 12 };
  private final int[] MANY = new int[200];

  @BeforeEach
  public void setup() {
    MANY[0] = 10;
    MANY[1] = 11;
    MANY[2] = 11;
    MANY[3] = 12;
    for (int i = 4; i < MANY.length; i++) {
      MANY[i] = MANY[i - 1] + 1;
    }
  }

  @Test
  void findDepartureSingleElementTimetable() {
    assertEquals(PREV_TIME_TABLE_INDEX, findBoardTime(ONE, 0, 1, 9));
    assertEquals(0, findBoardTime(ONE, 0, 1, 10));
    assertEquals(NEXT_TIME_TABLE_INDEX, findBoardTime(ONE, 0, 1, 11));
  }

  @Test
  void findDepartureInTimetableWithFourElements() {
    final int start = 0;
    final int end = FOUR.length;

    assertEquals(PREV_TIME_TABLE_INDEX, findBoardTime(FOUR, start, end, 9));
    assertEquals(0, findBoardTime(FOUR, start, end, 10));
    assertEquals(1, findBoardTime(FOUR, start, end, 11));
    assertEquals(3, findBoardTime(FOUR, start, end, 12));
    assertEquals(NEXT_TIME_TABLE_INDEX, findBoardTime(FOUR, start, end, 13));
  }

  @Test
  void findDepartureInTimetableWith200Elements() {
    testSearch(BoardTimeSearch::findBoardTime);
    testSearch(BoardTimeSearch::findBoardTimeLinearApproximation);
    testSearch(BoardTimeSearch::findBoardTimeBinarySearch);
  }

  void testSearch(TimeSearch search) {
    // Search the first half of timetable
    int start = 0;
    int end = start + 100;

    assertEquals(PREV_TIME_TABLE_INDEX, search.search(MANY, start, end, 9));
    assertEquals(0, search.search(MANY, start, end, 10));
    assertEquals(1, search.search(MANY, start, end, 11));
    assertEquals(3, search.search(MANY, start, end, 12));
    // Search time at end
    assertEquals(99, search.search(MANY, start, end, 108));
    assertEquals(NEXT_TIME_TABLE_INDEX, search.search(MANY, start, end, 109));

    // Search for all times to see that thresholds are done right
    for (int i = 4; i < end; ++i) {
      assertEquals(i, search.search(MANY, start, end, i + 9));
    }

    // Search the last half of timetable
    start = end;
    end = start + 100;

    for (int i = start + 1; i < end; ++i) {
      assertEquals(i, search.search(MANY, start, end, i + 9));
    }

    assertEquals(NEXT_TIME_TABLE_INDEX, search.search(MANY, start, end, 209));
  }

  /**
   * Make sure to set the two constants in {@link BoardTimeSearch} to value 10 before running this
   * test:
   * <ul>
   *   <li>{@code THRESHOLD_LINEAR_APPROXIMATION}</li>
   *   <li>{@code BINARY_SEARCH_THRESHOLD}</li>
   * </ul>
   * This test generates trip schedules with 10 to 1500 times in it and run all implemented search
   * strategies for each sample. The sample times are generated using a normal distribution
   * including all times within 1 and 2 standard deviations. As expected the binary search is best
   * when using standard deviation = 2, while the linear approximation is better when using
   * standard deviation = 1.
   */
  @Test
  @Disabled("Manual test")
  void compareSearchPerformance() {
    final int sampleSize = 4_000_000;
    final boolean skewDistribution = false;

    List
      // Run the fist sample twice to warm up the JIT compiler
      .of(8, 8, 10, 12, 16, 20)
      .forEach(threashold -> {
        // UNCOMMENT THIS AND MAKE THE CONSTANT WRITABLE BEFORE RUNNING THE TEST
        // BoardTimeSearch.THRESHOLD_LINEAR_APPROX_MIN_LIMIT = threashold;

        final Random rnd = new Random(13);
        List
          .of(2.0, 1.0)
          .forEach(stdDeviation -> {
            System.out.println("-----------------------------------------------------");
            System.out.println(
              "Standard distribution: " +
              stdDeviation +
              ", N=" +
              BoardTimeSearch.THRESHOLD_LINEAR_APPROX_MIN_LIMIT
            );
            System.out.printf("      %7s  %7s  %7s %n", "Linear", "Approx", "Binary");

            List
              .of(10, 12, 15, 20, 30, 50, 75, 100, 150, 200, 250, 350, 500, 750, 1000, 1500)
              .forEach(s -> {
                if (s <= BoardTimeSearch.THRESHOLD_LINEAR_APPROX_MIN_LIMIT) {
                  return;
                }

                int[] array = generateNormalDistributedData(rnd, stdDeviation, s, skewDistribution);

                // Linear search
                long startTime = System.currentTimeMillis();
                for (int t = 0; t < sampleSize; ++t) {
                  int v = array[t % s];
                  int x = findBoardTime(array, 0, array.length, v);
                }
                long timesLin = System.currentTimeMillis() - startTime;

                // Linear approximation
                startTime = System.currentTimeMillis();
                for (int t = 0; t < sampleSize; ++t) {
                  int v = array[t % s];
                  int x = findBoardTimeLinearApproximation(array, 0, array.length, v);
                }
                long timesApx = System.currentTimeMillis() - startTime;

                // Binary search
                startTime = System.currentTimeMillis();
                for (int t = 0; t < sampleSize; ++t) {
                  int v = array[t % s];
                  int x = findBoardTimeBinarySearch(array, 0, array.length, v);
                }
                long timesBin = System.currentTimeMillis() - startTime;

                System.out.printf("%4d  %5dms  %5dms  %5dms %n", s, timesLin, timesApx, timesBin);
              });
          });
      });
  }

  private int[] generateNormalDistributedData(
    Random rnd,
    Double stdDeviation,
    Integer s,
    boolean skewDistribution
  ) {
    int[] array = new int[s];
    for (int i = 0; i < array.length; i++) {
      double r = -1.0;
      while (r <= 0.0 || r > 1.0) {
        if (skewDistribution) {
          // "Half" normal distribution with the top in the beginning and then decreasing
          r = rnd.nextGaussian() / stdDeviation;
        } else {
          // Normal distribution with the top in the middle
          r = (rnd.nextGaussian() + stdDeviation) / (2.0 * stdDeviation);
        }
      }
      array[i] = (int) (r * 20.0 * 3600.0);
    }
    Arrays.sort(array);
    // printDistribution(array);
    return array;
  }

  @SuppressWarnings("unused")
  private void printDistribution(int[] array) {
    int min = Arrays.stream(array).min().getAsInt();
    int max = Arrays.stream(array).max().getAsInt();

    String[] dist = new String[10];
    double d = (max - min) / 10.0;
    int j = 0;

    for (int i = 0; i < 10; ++i) {
      int limit = (int) Math.round(d * (i + 1));
      dist[i] = "  <- %s  ".formatted(TimeUtils.timeToStrLong(limit));
      while (array[j] < limit) {
        dist[i] = dist[i] + "#";
        ++j;
      }
    }
    for (String line : dist) {
      System.out.println(line);
    }
  }
}
