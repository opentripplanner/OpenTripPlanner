package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.util.List;

/**
 * This class build an tripIndex for {@link TripPatternForDates} to (day, tripIndexForDay).
 * In most cases concatenating the depature times would be correct, but the last depature on a
 * specific day, may depart after the first departure on the following day. In these cases we need
 * to swap the two depatures in the timetable Raptor searches. The reason we need this, is that
 * trips on two diffrent datys may have overlapping trip-times. A nightbus on Saturdays may leave
 * at 04:05+1d, while the first bus on Sundays may leave at 03:00. Be aware, these two trips may
 * not have the same service calendar, normally they are diffrent. For example "Sundays" might be
 * all Sundays and public holidays.
 * </p>
 * The trip index is a sorted list of trips based on the first stop depature time. The index
 * contains two pointers, the first is the day and the second is the trip index on that day:
 * ```
 *   tripIndexForTripPatternPerDates -> (day, tripIndexForTripPatternPerDate)
 * ```
 * </p>
 * This class might at first look a bit complicated. A much easier approch would be to just sort
 * on depature times using a Java build in sort, but sorting is expencive. This code will merge
 * the  timetables instead - witch is faster. In 99% of the cases we will just do one extra
 * comparasons for each depature time.
 */
final class TripTimesForDaysIndex {

  private final int[] tripIndex;

  static TripTimesForDaysIndex ofTripTimesForDay(List<int[]> departureTimes, int[] offsets) {
    departureTimes = applyOffsets(departureTimes, offsets);
    return new TripTimesForDaysIndex(departureTimes);
  }

  TripTimesForDaysIndex(List<int[]> firstStopDepartureTimesPerDay) {
    // 'list' is an alias to make the logic below easier to read
    final List<int[]> list = firstStopDepartureTimesPerDay;
    this.tripIndex = new int[list.stream().mapToInt(a -> a.length).sum() * 2];
    int[] a;

    int day = 0;
    a = list.get(day);
    int tripIndexForDays = 0;
    int i = 0;
    int j = 0;
    int v = a[i];
    int u = safeValueNextDayAt(list, day, j);

    do {
      if (v <= u) {
        tripIndexForDays = setIndexValue(tripIndexForDays, day, i);
        ++i;
        if (i < a.length) {
          v = a[i];
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
          v = a[i];
          u = safeValueNextDayAt(list, day, j);
        }
      }
      // v > u
      else {
        tripIndexForDays = setIndexValue(tripIndexForDays, day + 1, j);
        ++j;
        u = safeValueNextDayAt(list, day, j);
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

  private int setIndexValue(int tripIndexForDays, int day, int tripIndexForDay) {
    this.tripIndex[tripIndexForDays * 2] = day;
    this.tripIndex[tripIndexForDays * 2 + 1] = tripIndexForDay;
    return tripIndexForDays + 1;
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
    var buf = new StringBuilder();
    for (int i = 0; i < tripIndex.length; i += 2) {
      buf.append(tripIndex[i]).append(':').append(tripIndex[i + 1]).append(' ');
    }
    return buf.substring(0, buf.length() - 1);
  }
}
