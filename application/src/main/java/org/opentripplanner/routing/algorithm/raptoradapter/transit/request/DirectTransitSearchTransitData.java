package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Set;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.IntIterators;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorPathConstrainedTransferSearch;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptor.spi.RaptorStopNameResolver;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.spi.RaptorTripScheduleReference;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.transit.model.basic.TransitMode;

/**
 * The transit data used by the direct transit search when it is limited to a set of transit
 * modes. Only the trip patterns with one of the given modes are visible to the search, everything
 * else is delegated to the transit data used by the main search.
 */
public class DirectTransitSearchTransitData implements RaptorTransitDataProvider<TripSchedule> {

  private final RaptorTransitDataProvider<TripSchedule> delegate;
  private final Set<TransitMode> modes;

  public DirectTransitSearchTransitData(
    RaptorTransitDataProvider<TripSchedule> delegate,
    Set<TransitMode> modes
  ) {
    this.delegate = delegate;
    this.modes = Set.copyOf(modes);
  }

  @Override
  public IntIterator routeIndexIterator(IntIterator stops) {
    var routes = delegate.routeIndexIterator(stops);
    var result = new BitSet();
    while (routes.hasNext()) {
      int routeIndex = routes.next();
      if (hasIncludedMode(delegate.getRouteForIndex(routeIndex))) {
        result.set(routeIndex);
      }
    }
    return IntIterators.of(result);
  }

  /**
   * All trips in a route share the same pattern, so the mode is read from the first trip.
   */
  private boolean hasIncludedMode(RaptorRoute<TripSchedule> route) {
    var timetable = route.timetable();
    return (
      timetable.numberOfTripSchedules() > 0 &&
      modes.contains(timetable.getTripSchedule(0).getOriginalTripPattern().getMode())
    );
  }

  @Override
  public void setup() {
    delegate.setup();
  }

  @Override
  public int numberOfStops() {
    return delegate.numberOfStops();
  }

  @Override
  public int numberOfTripPatterns() {
    return delegate.numberOfTripPatterns();
  }

  @Override
  public Iterator<? extends RaptorTransfer> getTransfersFromStop(int fromStop) {
    return delegate.getTransfersFromStop(fromStop);
  }

  @Override
  public Iterator<? extends RaptorTransfer> getTransfersToStop(int toStop) {
    return delegate.getTransfersToStop(toStop);
  }

  @Override
  public RaptorRoute<TripSchedule> getRouteForIndex(int routeIndex) {
    return delegate.getRouteForIndex(routeIndex);
  }

  @Override
  public RaptorCostCalculator<TripSchedule> multiCriteriaCostCalculator() {
    return delegate.multiCriteriaCostCalculator();
  }

  @Override
  public RaptorSlackProvider slackProvider() {
    return delegate.slackProvider();
  }

  @Override
  public RaptorPathConstrainedTransferSearch<TripSchedule> transferConstraintsSearch() {
    return delegate.transferConstraintsSearch();
  }

  @Override
  public RaptorStopNameResolver stopNameResolver() {
    return delegate.stopNameResolver();
  }

  @Override
  public int getValidTransitDataStartTime() {
    return delegate.getValidTransitDataStartTime();
  }

  @Override
  public int getValidTransitDataEndTime() {
    return delegate.getValidTransitDataEndTime();
  }

  @Override
  public RaptorConstrainedBoardingSearch<TripSchedule> transferConstraintsForwardSearch(
    int routeIndex
  ) {
    return delegate.transferConstraintsForwardSearch(routeIndex);
  }

  @Override
  public RaptorConstrainedBoardingSearch<TripSchedule> transferConstraintsReverseSearch(
    int routeIndex
  ) {
    return delegate.transferConstraintsReverseSearch(routeIndex);
  }

  @Override
  public RaptorTripScheduleReference tripScheduleReference(TripSchedule trip) {
    return delegate.tripScheduleReference(trip);
  }
}
