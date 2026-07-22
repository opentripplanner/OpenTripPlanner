package org.opentripplanner.transit.model._data;

import java.util.List;
import java.util.function.Consumer;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;

/**
 * Test factory for constructing minimal {@link TripTimes} instances.
 */
public class TripTimesForTest {

  private static final TransitRepositoryForTest REPO = TransitRepositoryForTest.of();

  /**
   * A minimal {@link ScheduledTripTimes} with two stops and no real-time updates.
   */
  public static TripTimes scheduled() {
    Trip trip = TransitRepositoryForTest.trip("test-trip").build();
    var stop1 = REPO.stop("A", 0.0, 0.0).build();
    var stop2 = REPO.stop("B", 0.0, 0.0).build();

    StopTime st1 = new StopTime();
    st1.setStop(stop1);
    st1.setArrivalTime(0);
    st1.setDepartureTime(0);
    st1.setStopSequence(0);

    StopTime st2 = new StopTime();
    st2.setStop(stop2);
    st2.setArrivalTime(60);
    st2.setDepartureTime(60);
    st2.setStopSequence(1);

    return TripTimesFactory.tripTimes(trip, List.of(st1, st2), new Deduplicator());
  }

  /**
   * A {@link org.opentripplanner.transit.model.timetable.RealTimeTripTimes} built from a
   * minimal scheduled base with additional state applied via {@code customizer}.
   *
   * <p>Example:
   * <pre>{@code
   * TripTimesForTest.realTime(builder -> builder.addTrip())
   * }</pre>
   */
  public static TripTimes realTime(Consumer<RealTimeTripTimesBuilder> customizer) {
    RealTimeTripTimesBuilder builder = scheduled().createRealTimeFromScheduledTimes();
    customizer.accept(builder);
    return builder.build();
  }
}
