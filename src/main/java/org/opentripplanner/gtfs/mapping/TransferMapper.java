package org.opentripplanner.gtfs.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripStopTimes;
import org.opentripplanner.model.transfer.StopTransferPoint;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.model.transfer.TransferPoint;
import org.opentripplanner.model.transfer.TransferPriority;
import org.opentripplanner.model.transfer.TripTransferPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for mapping GTFS Transfer into the OTP model.
 *
 * <p>This mapper is stateful and not thread safe. Create a new mapper for every set
 * of transfers you want to map.
 */
class TransferMapper {

  private static final Logger LOG = LoggerFactory.getLogger(TransferMapper.class);

  /**
   * This transfer is recommended over other transfers. The routing algorithm should prefer this
   * transfer compared to other transfers, for example by assigning a lower weight to it.
   */
  private static final int RECOMMENDED = 0;

  /**
   * This means the departing vehicle will wait for the arriving one and leave sufficient time for a
   * rider to transfer between routes.
   */
  private static final int GUARANTEED = 1;

  /**
   * This is a regular transfer that is defined in the transit data (as opposed to OpenStreetMap
   * data). In the case that both are present, this should take precedence. Because the the duration
   * of the transfer is given and not the distance, walk speed will have no effect on this.
   */
  private static final int MIN_TIME = 2;

  /**
   * Transfers between these stops (and route/trip) is not possible (or not allowed), even if a
   * transfer is already defined via OpenStreetMap data or in transit data.
   */
  private static final int FORBIDDEN = 3;


  private final RouteMapper routeMapper;

  private final StationMapper stationMapper;

  private final StopMapper stopMapper;

  private final TripMapper tripMapper;

  private final TripStopTimes stopTimesByTrip;

  TransferMapper(
      RouteMapper routeMapper,
      StationMapper stationMapper,
      StopMapper stopMapper,
      TripMapper tripMapper,
      TripStopTimes stopTimesByTrip
  ) {
    this.routeMapper = routeMapper;
    this.stationMapper = stationMapper;
    this.stopMapper = stopMapper;
    this.tripMapper = tripMapper;
    this.stopTimesByTrip = stopTimesByTrip;
  }

  static TransferPriority mapTypeToPriority(int type) {
    switch (type) {
      case FORBIDDEN:
        return TransferPriority.NOT_ALLOWED;
      case GUARANTEED:
      case MIN_TIME:
        return TransferPriority.ALLOWED;
      case RECOMMENDED:
        return TransferPriority.RECOMMENDED;
    }
    throw new IllegalArgumentException("Mapping missing for type: " + type);
  }

  Collection<Transfer> map(Collection<org.onebusaway.gtfs.model.Transfer> allTransfers) {
    List<Transfer> result = new ArrayList<>();

    for (org.onebusaway.gtfs.model.Transfer it : allTransfers) {
      result.addAll(map(it));
    }
    return result;
  }

  /**
   * Map from GTFS to OTP model, {@code null} safe.
   */
  Collection<Transfer> map(org.onebusaway.gtfs.model.Transfer original) {
    return original == null ? List.of() : doMap(original);
  }

  private Collection<Transfer> doMap(org.onebusaway.gtfs.model.Transfer rhs) {

    Trip fromTrip = tripMapper.map(rhs.getFromTrip());
    Trip toTrip = tripMapper.map(rhs.getToTrip());
    Route fromRoute = routeMapper.map(rhs.getFromRoute());
    Route toRoute = routeMapper.map(rhs.getToRoute());

    boolean guaranteed = rhs.getTransferType() == GUARANTEED;
    boolean staySeated = sameBlockId(fromTrip, toTrip);

    TransferPriority transferPriority = mapTypeToPriority(rhs.getTransferType());

    // TODO TGR - Create a SimpleTransfer for this se issue #3369
    int transferTime = rhs.getMinTransferTime();

    // If this transfer do not give any advantages in the routing, then drop it
    if(!guaranteed && !staySeated && transferPriority == TransferPriority.ALLOWED) {
      if(transferTime > 0) {
        LOG.info("Transfer skipped, issue #3369: " + rhs);
      }
      else {
        LOG.warn("Transfer skipped - no effect on routing: " + rhs);
      }
      return List.of();
    }

    // Transfers may be specified using parent stations
    // (https://developers.google.com/transit/gtfs/reference/transfers-file)
    // "If the stop ID refers to a station that contains multiple stops, this transfer rule
    // applies to all stops in that station." we thus expand transfers that use parent stations
    // to all the member stops.

    Collection<Stop> fromStops = getStopOrChildStops(rhs.getFromStop());
    Collection<Stop> toStops = getStopOrChildStops(rhs.getToStop());

    Collection<TransferPoint> fromPoints = mapTransferPoints(fromStops, fromTrip, fromRoute);
    Collection<TransferPoint> toPoints = mapTransferPoints(toStops, toTrip, toRoute);

    Collection<Transfer> result = new ArrayList<>();

    for (TransferPoint fromPoint : fromPoints) {
      for (TransferPoint toPoint : toPoints) {
        Transfer transfer = new Transfer(
                fromPoint,
                toPoint,
                transferPriority,
                staySeated,
                guaranteed,
                Transfer.MAX_WAIT_TIME_NOT_SET
        );
        result.add(transfer);
      }
    }
    return result;
  }

  private Collection<TransferPoint> mapTransferPoints(
      Collection<Stop> stops,
      Trip trip,
      Route route
  ) {
    Collection<TransferPoint> result = new ArrayList<>();
    if (trip != null) {
      result.addAll(createTransferPointForTrip(stops, trip, TripTransferPoint::new));
    }
    else if (route != null) {
      /*
      TODO - This code result in a OutOfMemory exception, fin out why and fix it
           - See issue https://github.com/opentripplanner/OpenTripPlanner/issues/3429
      for (Trip tripInRoute : tripsByRoute.get(route)) {
        result.addAll(
            createTransferPointForTrip(
              stops,
              tripInRoute,
              (t,i) -> new RouteTransferPoint(route, t, i)
            )
        );
      }
       */
    }
    else {
      for (Stop stop : stops) {
        result.add(new StopTransferPoint(stop));
      }

    }
    return result;
  }

  private Collection<TransferPoint> createTransferPointForTrip(
      Collection<Stop> stops,
      Trip trip,
      BiFunction<Trip, Integer, TransferPoint> createPoint
  ) {
    Collection<TransferPoint> result = new ArrayList<>();
    List<StopTime> stopTimes = stopTimesByTrip.get(trip);
    for (int i = 0; i < stopTimes.size(); ++i) {
      StopTime stopTime = stopTimes.get(i);

      //noinspection SuspiciousMethodCalls
      if (stops.contains(stopTime.getStop())) {
        result.add(createPoint.apply(trip, i));
      }
    }
    return result;
  }

  private Collection<Stop> getStopOrChildStops(org.onebusaway.gtfs.model.Stop gtfsStop) {
    if (gtfsStop.getLocationType() == 0) {
      return Collections.singletonList(stopMapper.map(gtfsStop));
    }
    else {
      return stationMapper.map(gtfsStop).getChildStops();
    }
  }

  private boolean sameBlockId(Trip a, Trip b) {
    if (a == null || b == null) {
      return false;
    }
    return a.getBlockId() != null && a.getBlockId().equals(b.getBlockId());
  }

  @Nullable
  private Map<Route,List<Trip>> createTripsByRouteMapIfRouteTransfersExist(
      Collection<Trip> trips,
      Collection<org.onebusaway.gtfs.model.Transfer> allTransfers
  ) {
    if(allTransfers.stream().anyMatch(t -> t.getFromRoute() != null || t.getToRoute() != null)) {
      return trips.stream().collect(Collectors.groupingBy(Trip::getRoute));
    }
    // Return null, not an empty map to enforce NPE if used when no Route exist
    return null;
  }
}
