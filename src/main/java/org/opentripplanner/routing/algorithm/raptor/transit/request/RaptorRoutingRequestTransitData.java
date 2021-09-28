package org.opentripplanner.routing.algorithm.raptor.transit.request;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptor.transit.cost.DefaultCostCalculator;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.McCostParamsMapper;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorConstrainedTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorPathConstrainedTransferSearch;
import org.opentripplanner.transit.raptor.api.transit.RaptorRoute;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransitDataProvider;
import org.opentripplanner.util.OTPFeature;


/**
 * This is the data provider for the Range Raptor search engine. It uses data from the TransitLayer,
 * but filters it by dates and modes per request. Transfers durations are pre-calculated per request
 * based on walk speed.
 */
public class RaptorRoutingRequestTransitData implements RaptorTransitDataProvider<TripSchedule> {

  private final TransitLayer transitLayer;

  private final TransferService transferService;

  /**
   * Active trip patterns by stop index
   */
  private final List<List<TripPatternForDates>> activeTripPatternsPerStop;

  /**
   * Transfers by stop index
   */
  private final List<List<RaptorTransfer>> transfers;

  private final ZonedDateTime startOfTime;

  private final CostCalculator generalizedCostCalculator;


  public RaptorRoutingRequestTransitData(
      TransferService transferService,
      TransitLayer transitLayer,
      Instant departureTime,
      int additionalFutureSearchDays,
      TransitDataProviderFilter filter,
      RoutingRequest routingRequest
  ) {

    this.transferService = transferService;
    this.transitLayer = transitLayer;

    // Delegate to the creator to construct the needed data structures. The code is messy so
    // it is nice to NOT have it in the class. It isolate this code to only be available at
    // the time of construction
    var transitDataCreator = new RaptorRoutingRequestTransitDataCreator(
            transitLayer, departureTime
    );
    this.startOfTime = transitDataCreator.getSearchStartTime();
    this.activeTripPatternsPerStop = transitDataCreator.createTripPatternsPerStop(
        additionalFutureSearchDays,
        filter
    );
    this.transfers = transitLayer.getRaptorTransfersForRequest(routingRequest);
    this.generalizedCostCalculator = new DefaultCostCalculator(
            McCostParamsMapper.map(routingRequest),
            transitLayer.getStopIndex().stopBoardAlightCosts
    );
  }

  /**
   * Gets all the transfers starting at a given stop
   */
  @Override
  public Iterator<RaptorTransfer> getTransfers(int stopIndex) {
    return transfers.get(stopIndex).iterator();
  }

  /**
   * Gets all the unique trip patterns touching a set of stops
   */
  @Override
  public Iterator<? extends RaptorRoute<TripSchedule>> routeIterator(IntIterator stops) {
    Set<RaptorRoute<TripSchedule>> activeTripPatternsForGivenStops = new HashSet<>();
    while (stops.hasNext()) {
      activeTripPatternsForGivenStops.addAll(activeTripPatternsPerStop.get(stops.next()));
    }
    return activeTripPatternsForGivenStops.iterator();
  }

  @Override
  public int numberOfStops() {
    return transitLayer.getStopCount();
  }

  @Override
  public CostCalculator multiCriteriaCostCalculator() {
    return generalizedCostCalculator;
  }

  @Override
  public RaptorPathConstrainedTransferSearch<TripSchedule> transferConstraintsSearch() {
    if(OTPFeature.TransferConstraints.isOff() || transferService == null) { return null; }

    return new RaptorPathConstrainedTransferSearch<>() {
      @Nullable @Override
      public RaptorConstrainedTransfer findConstrainedTransfer(
              TripSchedule fromTrip, int fromStopPosition, TripSchedule toTrip, int toStopPosition
      ) {
        return transferService.findTransfer(
                transitLayer.getStopByIndex(fromTrip.pattern().stopIndex(fromStopPosition)),
                transitLayer.getStopByIndex(toTrip.pattern().stopIndex(toStopPosition)),
                fromTrip.getOriginalTripTimes().getTrip(),
                toTrip.getOriginalTripTimes().getTrip(),
                fromStopPosition,
                toStopPosition
        );
      }
    };
  }

  @Override
  public IntFunction<String> stopIndexTranslatorForDebugging() {
    return (int stopIndex) -> {
      var s = transitLayer.getStopByIndex(stopIndex);
      return s==null ? "null" : s.getName() + "(" + stopIndex + ")";
    };
  }

  public ZonedDateTime getStartOfTime() {
    return startOfTime;
  }
}
