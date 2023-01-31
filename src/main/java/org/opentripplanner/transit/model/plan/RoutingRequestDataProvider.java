package org.opentripplanner.transit.model.plan;

import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import javax.annotation.Nonnull;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.path.RaptorStopNameResolver;
import org.opentripplanner.raptor.spi.CostCalculator;
import org.opentripplanner.raptor.spi.DefaultSlackProvider;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorPathConstrainedTransferSearch;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.util.BitSetIterator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultRaptorTransfer;
import org.opentripplanner.transit.model.calendar.PatternOnDay;
import org.opentripplanner.transit.model.calendar.TransitCalendar;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.network.RoutingTripPatternV2;
import org.opentripplanner.transit.model.trip.TripOnDay;
import org.opentripplanner.transit.service.StopModel;

/**
 * This is the transit request scoped service to perform a routing request.
 */

/**
 * Stops: 0..3
 *
 * Stop on route (stop indexes):
 *   R1:  1 - 2 - 3
 *
 * Schedule:
 *   R1: 00:01 - 00:03 - 00:05
 *
 * Access (toStop & duration):
 *   1  30s
 *
 * Egress (fromStop & duration):
 *   3  20s
 */

public class RoutingRequestDataProvider implements RaptorTransitDataProvider<TripOnDay> {

  private final int day;
  private final TransitCalendar transitCalendar;
  private final StopModel stopModel;

  private final CostCalculator<TripOnDay> costCalculator;

  private StopPatternIndex stopPatternIndex;

  private RaptorSlackProvider slackProvider = new DefaultSlackProvider(0, 120, 0);

  private Deduplicator deduplicator = new Deduplicator();

  public RoutingRequestDataProvider(
    int day,
    TransitCalendar transitCalendar,
    StopModel stopModel,
    CostCalculator<TripOnDay> costCalculator
  ) {
    this.day = day;
    this.transitCalendar = transitCalendar;
    this.stopModel = stopModel;
    this.costCalculator = costCalculator;

    // TODO RTM - Insert patterns to build an index. we will need a better
    //          - why to do this later when we have realTime

    Collection<RoutingTripPatternV2> routingTripPatterns = transitCalendar
      .patternsOnDay(day)
      .stream()
      .map(PatternOnDay::pattern)
      .toList();

    this.stopPatternIndex =
      new StopPatternIndex(stopModel.stopIndexSize(), routingTripPatterns, deduplicator);
  }

  @Override
  public int numberOfStops() {
    return stopModel.stopIndexSize();
  }

  @Override
  public IntIterator routeIndexIterator(IntIterator stops) {
    BitSet patterns = stopPatternIndex.activePatternsByStops(stops);
    // TODO RTM - Use StopPatternIndex to compute the active patterns for the given stops
    //          - 1) Then filter the bitset using TransitCalendar
    //          - 2) Then filter the bitset using Request
    return new BitSetIterator(patterns);
  }

  @Override
  public RaptorRoute<TripOnDay> getRouteForIndex(int patternIndex) {
    // TODO RTM - We need to know witch day we should start
    //          - Fetch: transitCalendar -> PatternsOnDays#day -> PatternsOnDay#patternIndex -> PatternOnDay
    return new RaptorRouteAdaptor(null);
  }

  @Override
  public RaptorPathConstrainedTransferSearch<TripOnDay> transferConstraintsSearch() {
    //TODO RTM - Only needed to support constrained-transfer - ok for now
    return null;
  }

  @Override
  public Iterator<? extends RaptorTransfer> getTransfersFromStop(int fromStop) {
    // TODO RTM - Replace this with real transfer, we should be able to do single
    //          - transit leg routing with this
    return Collections.<DefaultRaptorTransfer>emptyIterator();
  }

  @Override
  public Iterator<? extends RaptorTransfer> getTransfersToStop(int toStop) {
    // TODO RTM - Replace this with real transfer, we should be able to do single
    //          - transit leg routing with this
    return Collections.<DefaultRaptorTransfer>emptyIterator();
  }

  @Override
  public CostCalculator<TripOnDay> multiCriteriaCostCalculator() {
    return costCalculator;
  }

  @Override
  public RaptorSlackProvider slackProvider() {
    return slackProvider;
  }

  @Nonnull
  @Override
  public RaptorStopNameResolver stopNameResolver() {
    return stopIndex -> stopModel.stopByIndex(stopIndex).logName();
  }

  @Override
  public int getValidTransitDataStartTime() {
    //TODO RTM - Ok for now
    return 0;
  }

  @Override
  public int getValidTransitDataEndTime() {
    //TODO RTM - OK for now
    return Integer.MAX_VALUE;
  }

  @Override
  public RaptorConstrainedBoardingSearch<TripOnDay> transferConstraintsForwardSearch(
    int routeIndex
  ) {
    //TODO RTM - Only needed to support constrained-transfer - ok for now
    return null;
  }

  @Override
  public RaptorConstrainedBoardingSearch<TripOnDay> transferConstraintsReverseSearch(
    int routeIndex
  ) {
    //TODO RTM - Only needed to support constrained-transfer - ok for now
    return null;
  }
}
