package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorRoute;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransitDataProvider;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * This is the data provider for the Range Raptor search engine. It uses data from the TransitLayer,
 * but filters it by dates and modes per request. Transfers durations are pre-calculated per request
 * based on walk speed.
 */
public class RaptorRoutingRequestTransitData implements RaptorTransitDataProvider<TripSchedule> {

  private final TransitLayer transitLayer;

  /**
   * Active trip patterns by stop index
   */
  private final List<List<TripPatternForDates>> activeTripPatternsPerStop;

  /**
   * Transfers by stop index
   */
  private final List<List<RaptorTransfer>> transfers;


  private final ZonedDateTime startOfTime;

  public RaptorRoutingRequestTransitData(
      TransitLayer transitLayer,
      Instant departureTime,
      int dayRange,
      Set<TransitMode> transitModes,
      Set<FeedScopedId> bannedRoutes,
      double walkSpeed
  ) {
    // Delegate to the creator to construct the needed data structures. The code is messy so
    // it is nice to NOT have it in the class. It isolate this code to only be available at
    // the time of construction
    RaptorRoutingRequestTransitDataCreator creator = new RaptorRoutingRequestTransitDataCreator(
        transitLayer,
        departureTime
    );

    this.transitLayer = transitLayer;
    this.startOfTime = creator.getSearchStartTime();
    this.activeTripPatternsPerStop = creator.createTripPatternsPerStop(
        dayRange,
        transitModes,
        bannedRoutes
    );
    this.transfers = creator.calculateTransferDuration(walkSpeed);
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
  public int[] stopBoarAlightCost() {
    return transitLayer.getStopIndex().stopBoardAlightCosts;
  }

  public ZonedDateTime getStartOfTime() {
    return startOfTime;
  }
}
