package org.opentripplanner.transit.model.plan;

import java.util.BitSet;
import java.util.Iterator;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripPattern;
import org.opentripplanner.raptor.api.path.RaptorStopNameResolver;
import org.opentripplanner.raptor.spi.CostCalculator;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorPathConstrainedTransferSearch;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.util.BitSetIterator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.CostCalculatorFactory;
import org.opentripplanner.transit.model.api.TransitRoutingRequest;
import org.opentripplanner.transit.model.calendar.TransitCalendar;
import org.opentripplanner.transit.model.transfers.StreetTransfers;
import org.opentripplanner.transit.model.trip.TripOnDate;
import org.opentripplanner.transit.model.trip.TripService;

/**
 * This is the transit request scoped service to perform a routing request.
 */
public class RoutingRequestDataProvider implements RaptorTransitDataProvider<TripOnDate> {

  private final TransitRoutingRequest request;
  private final TransitCalendar transitCalendar;
  private final TransitLayer transitLayer;
  private final TripService tripService;
  private final StreetTransfers transfersFromStop;
  private final StreetTransfers transfersToStop;
  private final CostCalculator generalizedCostCalculator;
  private final BitSet filteredPatternIndexes;

  public RoutingRequestDataProvider(
    TransitRoutingRequest request,
    TransitCalendar transitCalendar,
    TransitLayer transitLayer,
    TripService tripService,
    StreetTransfers transfersFromStop,
    StreetTransfers transfersToStop,
    BitSet filteredPatternIndexes
  ) {
    this.request = request;
    this.transitCalendar = transitCalendar;
    this.transitLayer = transitLayer;
    this.tripService = tripService;
    this.transfersFromStop = transfersFromStop;
    this.transfersToStop = transfersToStop;
    this.filteredPatternIndexes = filteredPatternIndexes;
    this.generalizedCostCalculator =
      CostCalculatorFactory.createCostCalculator(
        request.generalizedCostParams(),
        transitLayer.getStopBoardAlightCosts()
      );
  }

  @Override
  public int numberOfStops() {
    return transitLayer.getStopCount();
  }

  @Override
  public IntIterator routeIndexIterator(IntIterator stops) {
    BitSet patternMask = new BitSet(tripService.numberOfRoutingPatterns());

    while (stops.hasNext()) {
      // TODO RTM
      //patternMask.or(transitLayer.patternMaskForStop(stops.next()));
    }
    // Filter patterns based on the request (time, mode, operator ...)
    patternMask.and(filteredPatternIndexes);

    return new BitSetIterator(patternMask);
  }

  @Override
  public RaptorRoute<TripOnDate> getRouteForIndex(int routeIndex) {
    RaptorTripPattern pattern = tripService.routingPatterns(routeIndex);

    return new RaptorRouteAdaptor(pattern, /* TODO RTM */null);
  }

  @Override
  public RaptorPathConstrainedTransferSearch<TripOnDate> transferConstraintsSearch() {
    // TODO RTM
    return null;
  }

  @Override
  public Iterator<? extends RaptorTransfer> getTransfersFromStop(int fromStop) {
    return transfersFromStop.withStop(fromStop);
  }

  @Override
  public Iterator<? extends RaptorTransfer> getTransfersToStop(int toStop) {
    return transfersToStop.withStop(toStop);
  }

  @Override
  public CostCalculator multiCriteriaCostCalculator() {
    return generalizedCostCalculator;
  }

  @Override
  public RaptorSlackProvider slackProvider() {
    return null;
  }

  @Override
  public RaptorStopNameResolver stopNameResolver() {
    return stopIndex -> {
      var s = transitLayer.getStopByIndex(stopIndex);
      return s == null ? "null" : s.getName() + "(" + stopIndex + ")";
    };
  }

  @Override
  public int getValidTransitDataStartTime() {
    return request.validTransitDataStartTime();
  }

  @Override
  public int getValidTransitDataEndTime() {
    return request.validTransitDataEndTime();
  }

  @Override
  public RaptorConstrainedBoardingSearch<TripOnDate> transferConstraintsForwardSearch(
    int routeIndex
  ) {
    return null;
  }

  @Override
  public RaptorConstrainedBoardingSearch<TripOnDate> transferConstraintsReverseSearch(
    int routeIndex
  ) {
    return null;
  }
}
