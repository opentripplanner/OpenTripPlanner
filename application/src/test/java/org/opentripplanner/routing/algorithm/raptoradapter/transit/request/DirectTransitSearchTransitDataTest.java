package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.IntIterators;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;

class DirectTransitSearchTransitDataTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();

  @SuppressWarnings("unchecked")
  private final RaptorTransitDataProvider<TripSchedule> delegate = mock(
    RaptorTransitDataProvider.class
  );

  /** The route index of each route is its position in this list */
  private final List<TransitMode> routeModes = List.of(
    TransitMode.BUS,
    TransitMode.RAIL,
    TransitMode.FERRY
  );

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setup() {
    var routes = new BitSet();
    for (int routeIndex = 0; routeIndex < routeModes.size(); routeIndex++) {
      var mode = routeModes.get(routeIndex);
      var trip = mock(TripSchedule.class);
      when(trip.getOriginalTripPattern()).thenReturn(createTripPattern(mode));
      RaptorTimeTable<TripSchedule> timetable = mock(RaptorTimeTable.class);
      when(timetable.numberOfTripSchedules()).thenReturn(1);
      when(timetable.getTripSchedule(0)).thenReturn(trip);
      RaptorRoute<TripSchedule> route = mock(RaptorRoute.class);
      when(route.timetable()).thenReturn(timetable);
      when(delegate.getRouteForIndex(routeIndex)).thenReturn(route);
      routes.set(routeIndex);
    }
    when(delegate.routeIndexIterator(any())).thenAnswer(i -> IntIterators.of(routes));
  }

  @Test
  void routeIndexIteratorOnlyIncludesGivenModes() {
    var subject = new DirectTransitSearchTransitData(
      delegate,
      Set.of(TransitMode.BUS, TransitMode.FERRY)
    );

    var result = toList(subject.routeIndexIterator(IntIterators.empty()));

    assertThat(result).containsExactly(0, 2);
  }

  @Test
  void routeIndexIteratorIsEmptyWhenNoModeMatch() {
    var subject = new DirectTransitSearchTransitData(delegate, Set.of(TransitMode.TRAM));

    var result = toList(subject.routeIndexIterator(IntIterators.empty()));

    assertThat(result).isEmpty();
  }

  private static List<Integer> toList(IntIterator it) {
    var result = new ArrayList<Integer>();
    while (it.hasNext()) {
      result.add(it.next());
    }
    return result;
  }

  private static TripPattern createTripPattern(TransitMode mode) {
    return TripPattern.of(id(mode.name()))
      .withRoute(TransitRepositoryForTest.route(mode.name()).withMode(mode).build())
      .withStopPattern(new StopPattern(List.of(createStopTime(), createStopTime())))
      .build();
  }

  private static StopTime createStopTime() {
    var stopTime = new StopTime();
    stopTime.setStop(TEST_MODEL.stop("Stop:1", 0.0, 0.0).build());
    return stopTime;
  }
}
