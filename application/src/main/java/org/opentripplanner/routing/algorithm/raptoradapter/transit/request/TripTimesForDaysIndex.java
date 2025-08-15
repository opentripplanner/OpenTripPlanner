package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.util.List;

/**
 * This class builds a tripIndex for {@link TripPatternForDates} to (day, tripIndexForDay).
 * In most cases concatenating the departure times would be correct, but the last departure on a
 * specific day, may depart after the first departure on the following day. In these cases we need
 * to swap the two departures in the timetable Raptor searches. The reason we need this, is that
 * trips on two different days may have overlapping trip-times. A night bus on Saturdays may leave
 * at 04:05+1d, while the first bus on Sundays may leave at 03:00. Be aware, these two trips may
 * not have the same service calendar, normally they are different. For example "Sundays" might be
 * all Sundays and public holidays.
 * </p>
 * The trip index is a sorted list of trips based on the first stop departure time. The index
 * contains two pointers, the first is the day and the second is the trip index on that day:
 * ```
 *   tripIndexForTripPatternPerDates -> (day, tripIndexForTripPatternPerDate)
 * ```
 * </p>
 * This class might at first look a bit complicated. A much easier approach would be to just sort
 * on departure times using a Java built in sort, but sorting is expensive. This code will merge
 * the  timetables instead - which is faster. In 99% of the cases we will just do one extra
 * comparison for each departure time.
 */
final class TripTimesForDaysIndex {

  private final int[] tripIndex;

  /**
   * Build a trip index over the provided trip departure times per day and offsets. The trip order
   * is trip(i) <= trip(i+1) (departure-time for the first stop).
   */
  static TripTimesForDaysIndex ofTripTimesForDay(List<int[]> departureTimes, int[] offsets) {
    departureTimes = applyOffsets(departureTimes, offsets);
    return new TripTimesForDaysIndex(departureTimes);
  }

  /**
   * Goal: Read the JavaDoc for the class and factory method, and study the unit-test.
   *
   * Implementation notes!
   * <p>
   * We need to order trips based on the "actual" departure time, not day and departure-time. We do
   * this by merging the trip-departure times for each day. Each day is already sorted, so we need
   * to merge the end of each 'day' with the start of the next 'day+1'. We avoid sorting for
   * performance reasons.
   */
  TripTimesForDaysIndex(List<int[]> firstStopDepartureTimesPerDay) {
    // 'list' is an alias to make the logic below easier to read
    final List<int[]> list = firstStopDepartureTimesPerDay;
    this.tripIndex = new int[list.stream().mapToInt(a -> a.length).sum() * 2];
    int[] a;

    // 'day' is the current day index
    // 'i' and 'j' is the trip index for the current and next day
    // 'u' and 'v' is the departure time for the current trip on the current day and next day
    // 'tripIndexIndex' points to the next element to set in the target 'tripIndex'
    int day = 0;
    a = list.get(day);
    int tripIndexIndex = 0;
    int i = 0;
    int j = 0;
    int u = safeValueAt(a, i);
    int v = safeValueNextDayAt(list, day, j);

    do {
      if (u <= v) {
        if (i < a.length) {
          tripIndexIndex = setIndexValue(tripIndexIndex, day, i);
        }
        ++i;
        if (i < a.length) {
          u = a[i];
        } else {
          ++day;
          if (day == list.size()) {
            break;
          }
          i = j;
          a = list.get(day);
          if (i == a.length) {
            ++day;
            if (day == list.size()) {
              break;
            }
            i = 0;
            a = list.get(day);
          }
          j = 0;
          u = safeValueAt(a, i);
          v = safeValueNextDayAt(list, day, j);
        }
      }
      // v > u
      else {
        tripIndexIndex = setIndexValue(tripIndexIndex, day + 1, j);
        ++j;
        v = safeValueNextDayAt(list, day, j);
      }
    } while (true);
  }

  private static int safeValueNextDayAt(List<int[]> list, int day, int trip) {
    int nextDay = day + 1;
    return nextDay == list.size() ? Integer.MAX_VALUE : safeValueAt(list.get(nextDay), trip);
  }

  private static int safeValueAt(int[] array, int index) {
    return array.length == index ? Integer.MAX_VALUE : array[index];
  }

  int day(int tripIndexForDays) {
    return this.tripIndex[tripIndexForDays * 2];
  }

  int tripIndexForDay(int tripIndexForDays) {
    return tripIndex[tripIndexForDays * 2 + 1];
  }

  int size() {
    return tripIndex.length / 2;
  }

  private int setIndexValue(int tripIndexIndex, int day, int tripIndexForDay) {
    this.tripIndex[tripIndexIndex * 2] = day;
    this.tripIndex[tripIndexIndex * 2 + 1] = tripIndexForDay;
    return tripIndexIndex + 1;
  }

  static List<int[]> applyOffsets(List<int[]> list, int[] offsets) {
    for (int i = 0; i < list.size(); ++i) {
      int o = offsets[i];
      int[] a = list.get(i);
      for (int j = 0; j < a.length; ++j) {
        a[j] = a[j] + o;
      }
    }
    return list;
  }

  @Override
  public String toString() {
    if (tripIndex.length == 0) {
      return "";
    }
    var buf = new StringBuilder();
    for (int i = 0; i < tripIndex.length; i += 2) {
      buf.append(tripIndex[i]).append(':').append(tripIndex[i + 1]).append(' ');
    }
    return buf.substring(0, buf.length() - 1);
  }
}
