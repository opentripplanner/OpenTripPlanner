package org.opentripplanner.transit.model.trip.timetable;

import static org.opentripplanner.transit.model.trip.timetable.TimetableIntUtils.collapseAndFlipMatrix;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import org.opentripplanner.framework.lang.IntUtils;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.trip.Timetable;

/**
 * This class is intended for building Timetables. It mau analyze the data before building and
 * produce the timetable with the best (speed/memory) performance depending on the data.
 * <p>
 * THIS CLASS IS RESPONSIBLE FOR VALIDATING THE TIMETABLE BEFORE CREATING THE INSTANCE.
 * The reason we do it here and not in the timetable is that we want to implement more
 * than one {@link Timetable} - depending on the nature of the timetable. For example,
 * we can have an efficient implementation if all hop/dwell times are static and only
 * the departure "offset" is different - like a frequency based timetable.
 */
public class TimetableBuilder {

  private final Deduplicator deduplicator;

  private final List<BoarAlightTimes> times = new ArrayList<>();

  public TimetableBuilder(Deduplicator deduplicator) {
    this.deduplicator = deduplicator;
  }

  private TimetableBuilder() {
    this(new Deduplicator());
  }

  public static TimetableBuilder of() {
    return new TimetableBuilder();
  }

  /**
   * Parse the given {@code schedule} and populate both board and alight times with the given
   * schedule. The board- and alight- time for a given stop will be the same.
   * <p>
   * Expected input format: {@code 09:30 10:22 12:33 16:59:59 17:00}
   */
  public TimetableBuilder schedule(String schedule) {
    int[] times = TimeUtils.times(schedule);
    this.times.add(new BoarAlightTimes(times, times));
    return this;
  }

  /**
   * Parse the given {@code schedule} and populate both board and alight times. The board times
   * is given and the alight times are computed so a {@code boardTime + dwellTime == alightTime}.
   * <p>
   * Expected input format: {@code 09:30 10:22 12:33 16:59:59 17:00}
   */
  public TimetableBuilder schedule(String boardSchedule, int dwellTimeSeconds) {
    int[] times = TimeUtils.times(boardSchedule);
    this.times.add(new BoarAlightTimes(times, IntUtils.arrayPlus(times, -dwellTimeSeconds)));
    return this;
  }

  /**
   * Parse the given {@code board-/alight-times} and populate board and alight times with the given
   * schedule.
   * <p>
   * Expected input format: {@code 09:30 10:22 12:33 16:59:59 17:00}
   */
  public TimetableBuilder schedule(String boardTimes, String alightTimes) {
    this.times.add(new BoarAlightTimes(TimeUtils.times(boardTimes), TimeUtils.times(alightTimes)));
    return this;
  }

  public Timetable build() {
    if (times.isEmpty()) {
      throw new IllegalStateException("No board or alight times added.");
    }
    int nStops = times.get(0).boardTimes().length;
    for (BoarAlightTimes it : times) {
      if (it.nStops() != nStops) {
        throw new IllegalArgumentException(
          "All board times must have the same number of stops. " + nStops + " != " + it.nStops()
        );
      }
    }

    times.sort(Comparator.comparingInt(l -> l.boardTimes[0]));
    int[] boardTimes = collapseAndFlipMatrix(mapToArray(times, BoarAlightTimes::boardTimes));
    int[] alightTimes = collapseAndFlipMatrix(mapToArray(times, BoarAlightTimes::alightTimes));

    if (deduplicator != null) {
      boardTimes = deduplicator.deduplicateIntArray(boardTimes);
      alightTimes = deduplicator.deduplicateIntArray(alightTimes);
    }

    return new DefaultTimetable(nTrips(), nStops, boardTimes, alightTimes);
  }

  private int nTrips() {
    return times.size();
  }

  private static int[][] mapToArray(
    List<BoarAlightTimes> values,
    Function<BoarAlightTimes, int[]> getArray
  ) {
    final int size = values.size();
    int[][] array = new int[size][];
    for (int i = 0; i < size; ++i) {
      array[i] = getArray.apply(values.get(i));
    }
    return array;
  }

  private record BoarAlightTimes(int[] boardTimes, int[] alightTimes) {
    BoarAlightTimes {
      if (boardTimes.length != alightTimes.length) {
        throw new IllegalArgumentException(
          "Board and alight times must have the same number of stops. Board times=" +
          boardTimes.length +
          ", alight times=" +
          alightTimes.length
        );
      }
      if (boardTimes.length < 2) {
        throw new IllegalArgumentException("A trip schedule must have at least 2 stops.");
      }
    }

    int nStops() {
      return boardTimes.length;
    }
  }
}
