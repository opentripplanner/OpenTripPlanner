package org.opentripplanner.raptor._data.transit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.utils.lang.StringUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class TestRoute implements RaptorRoute<TestTripSchedule>, RaptorTimeTable<TestTripSchedule> {

  private final TestTripPattern pattern;
  private final List<TestTripSchedule> schedules = new ArrayList<>();
  private final TestConstrainedBoardingSearch transferConstraintsForwardSearch =
    new TestConstrainedBoardingSearch(true);
  private final TestConstrainedBoardingSearch transferConstraintsReverseSearch =
    new TestConstrainedBoardingSearch(false);

  private TestRoute(TestTripPattern pattern) {
    this.pattern = pattern;
  }

  public static TestRoute route(TestTripPattern pattern) {
    return new TestRoute(pattern);
  }

  public static TestRoute route(String name, int... stopIndexes) {
    return route(TestTripPattern.pattern(name, stopIndexes));
  }

  public static Builder route(String name) {
    return new Builder(name);
  }

  /* RaptorRoute */

  @Override
  public RaptorTimeTable<TestTripSchedule> timetable() {
    return this;
  }

  @Override
  public TestTripPattern pattern() {
    return pattern;
  }

  public RaptorConstrainedBoardingSearch<TestTripSchedule> transferConstraintsForwardSearch() {
    return transferConstraintsForwardSearch;
  }

  public RaptorConstrainedBoardingSearch<TestTripSchedule> transferConstraintsReverseSearch() {
    return transferConstraintsReverseSearch;
  }

  /* RaptorTimeTable */

  @Override
  public TestTripSchedule getTripSchedule(int index) {
    return schedules.get(index);
  }

  @Override
  public int numberOfTripSchedules() {
    return schedules.size();
  }

  @Override
  public RaptorTripScheduleSearch<TestTripSchedule> tripSearch(SearchDirection direction) {
    return new TestTripScheduleSearch(direction, schedules);
  }

  public List<TestConstrainedTransfer> listTransferConstraintsForwardSearch() {
    return transferConstraintsForwardSearch.constrainedBoardings();
  }

  public TestRoute withTimetable(TestTripSchedule... trips) {
    Collections.addAll(schedules, trips);
    return this;
  }

  public TestRoute withTimetable(TestTripSchedule.Builder... scheduleBuilders) {
    for (TestTripSchedule.Builder builder : scheduleBuilders) {
      var tripSchedule = builder.pattern(pattern).build();
      schedules.add(tripSchedule);
    }
    return this;
  }

  public TestRoute withTimetable(String timetable) {
    Arrays.stream(timetable.split("\\n"))
      .filter(StringUtils::hasValue)
      .map(s -> TestTripSchedule.schedule(s).pattern(pattern).build())
      .forEach(schedules::add);
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TestRoute.class)
      .addObj("pattern", pattern)
      .addObj("schedules", schedules)
      .toString();
  }

  void clearTransferConstraints() {
    transferConstraintsForwardSearch.clear();
    transferConstraintsReverseSearch.clear();
  }

  /**
   * Add a transfer constraint to the route by iterating over all trips and matching the provided
   * {@code toTrip}(added to forward search) {@code fromTrip}(added to reverse search) with the rips
   * in the route timetable.
   */
  void addTransferConstraint(
    TestTripSchedule fromTrip,
    int fromStopPos,
    TestTripSchedule toTrip,
    int toStopPos,
    TestTransferConstraint constraint
  ) {
    for (int i = 0; i < timetable().numberOfTripSchedules(); i++) {
      var trip = timetable().getTripSchedule(i);
      if (toTrip == trip) {
        this.transferConstraintsForwardSearch.addConstraintTransfers(
            fromTrip,
            fromStopPos,
            trip,
            i,
            toStopPos,
            trip.arrival(toStopPos),
            constraint
          );
      }
      // Reverse search transfer, the {@code source/target} is the trips in order of the
      // reverse search, which is opposite from {@code from/to} in the result path.
      if (fromTrip == trip) {
        this.transferConstraintsReverseSearch.addConstraintTransfers(
            toTrip,
            toStopPos,
            trip,
            i,
            fromStopPos,
            trip.departure(fromStopPos),
            constraint
          );
      }
    }
  }

  public static final class Builder {

    private String name;

    public Builder(String name) {
      this.name = name;
    }

    /**
     * Create a route with the given stop-pattern and schedule parsing the given {@code timetable}.
     * The format of the timetable is:
     * <pre>
     *   A      B      C      F
     * 10:00  10:20  10:25  10:45
     * 11:00  11:20  11:25  11:45
     * 12:00  12:20  12:25  12:45
     * </pre>
     * This will create a timetable with for stops(A, B, C, & F) and 3 scheduled trips. The
     * {@link RaptorTestConstants#stopNameToIndex(String)} is used to resolve the stop index
     * for each of the named stops A, B, C & F. The first line must contain the stop names(A-Z),
     * and the each extra line is the trips. Extra white-space and empty lines are ignored.
     */
    public TestRoute timetable(String timetable) {
      timetable = timetable.trim();
      int end = timetable.indexOf('\n');
      var stopIndexes = Arrays.stream(timetable.substring(0, end).split("\\s+"))
        .mapToInt(RaptorTestConstants::stopNameToIndex)
        .toArray();
      return route(name, stopIndexes).withTimetable(timetable.substring(end + 1));
    }
  }
}
