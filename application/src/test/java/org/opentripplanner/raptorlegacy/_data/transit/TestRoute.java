package org.opentripplanner.raptorlegacy._data.transit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.routing.algorithm.raptoradapter.api.DefaultTripPattern;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TripScheduleSearchFactory;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * @deprecated This was earlier part of Raptor and should not be used outside the Raptor
 *             module. Use the OTP model entities instead.
 */
@Deprecated
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

  /* RaptorRoute */

  @Override
  public RaptorTimeTable<TestTripSchedule> timetable() {
    return this;
  }

  @Override
  public DefaultTripPattern pattern() {
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
    return TripScheduleSearchFactory.create(direction, new TestTripSearchTimetable(this));
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
    TransferConstraint constraint
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
}
