package org.opentripplanner.gtfs.mapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import org.onebusaway.gtfs.model.Transfer;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.IgnoredGtfsTransfer;
import org.opentripplanner.graph_builder.issues.InvalidGtfsTransfer;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TripStopTimes;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.RouteStationTransferPoint;
import org.opentripplanner.model.transfer.RouteStopTransferPoint;
import org.opentripplanner.model.transfer.StationTransferPoint;
import org.opentripplanner.model.transfer.StopTransferPoint;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.model.transfer.TransferPoint;
import org.opentripplanner.model.transfer.TransferPriority;
import org.opentripplanner.model.transfer.TripTransferPoint;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Responsible for mapping GTFS Transfer into the OTP model.
 *
 * <p>This mapper is stateful and not thread safe. Create a new mapper for every set
 * of transfers you want to map.
 */
class TransferMapper {

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

  /**
   * Passengers can transfer from one trip to another by staying onboard the same vehicle.
   *
   * @see <a href="https://github.com/google/transit/pull/303">GTFS proposal</a>
   */
  private static final int STAY_SEATED = 4;

  /**
   * In-seat transfers are not allowed between sequential trips. The passenger must alight from the
   * vehicle and re-board.
   *
   * @see <a href="https://github.com/google/transit/pull/303">GTFS proposal</a>
   */
  private static final int STAY_SEATED_NOT_ALLOWED = 5;

  private final RouteMapper routeMapper;

  private final StationMapper stationMapper;

  private final StopMapper stopMapper;

  private final TripMapper tripMapper;

  private final TripStopTimes stopTimesByTrip;
  private final DataImportIssueStore issueStore;

  private final Multimap<Route, Trip> tripsByRoute = ArrayListMultimap.create();
  private final boolean discardMinTransferTimes;

  TransferMapper(
    RouteMapper routeMapper,
    StationMapper stationMapper,
    StopMapper stopMapper,
    TripMapper tripMapper,
    TripStopTimes stopTimesByTrip,
    boolean discardMinTransferTimes,
    DataImportIssueStore issueStore
  ) {
    this.routeMapper = routeMapper;
    this.stationMapper = stationMapper;
    this.stopMapper = stopMapper;
    this.tripMapper = tripMapper;
    this.stopTimesByTrip = stopTimesByTrip;
    this.discardMinTransferTimes = discardMinTransferTimes;
    this.issueStore = issueStore;
  }

  static TransferPriority mapTypeToPriority(int type) {
    switch (type) {
      case FORBIDDEN:
        return TransferPriority.NOT_ALLOWED;
      case GUARANTEED:
      case MIN_TIME:
      case STAY_SEATED:
      case STAY_SEATED_NOT_ALLOWED:
        return TransferPriority.ALLOWED;
      case RECOMMENDED:
        return TransferPriority.RECOMMENDED;
    }
    throw new IllegalArgumentException("Mapping missing for type: " + type);
  }

  TransferMappingResult map(Collection<org.onebusaway.gtfs.model.Transfer> allTransfers) {
    setup(!allTransfers.isEmpty());

    List<ConstrainedTransfer> constrainedTransfers = allTransfers
      .stream()
      .map(this::map)
      .filter(Objects::nonNull)
      .toList();

    List<StaySeatedNotAllowed> staySeatedNotAllowed = allTransfers
      .stream()
      .map(this::toStaySeatedNotAllowed)
      .filter(Objects::nonNull)
      .toList();

    return new TransferMappingResult(constrainedTransfers, staySeatedNotAllowed);
  }

  private StaySeatedNotAllowed toStaySeatedNotAllowed(Transfer t) {
    Trip fromTrip = tripMapper.map(t.getFromTrip());
    Trip toTrip = tripMapper.map(t.getToTrip());
    if (t.getTransferType() == STAY_SEATED_NOT_ALLOWED) {
      return new StaySeatedNotAllowed(fromTrip, toTrip);
    } else return null;
  }

  ConstrainedTransfer map(org.onebusaway.gtfs.model.Transfer rhs) {
    Trip fromTrip = tripMapper.map(rhs.getFromTrip());
    Trip toTrip = tripMapper.map(rhs.getToTrip());

    TransferConstraint constraint = mapConstraint(rhs);

    // If this transfer do not give any advantages in the routing, then drop it
    if (constraint.isRegularTransfer()) {
      issueStore.add(new IgnoredGtfsTransfer(rhs));
      return null;
    }

    if (constraint.isStaySeated() && (fromTrip == null || toTrip == null)) {
      issueStore.add(
        new InvalidGtfsTransfer("from_trip_id and to_trip_id must exist for in-seat transfer", rhs)
      );
      return null;
    }

    TransferPoint fromPoint = mapTransferPoint(
      rhs.getFromStop(),
      rhs.getFromRoute(),
      fromTrip,
      false
    );
    TransferPoint toPoint = mapTransferPoint(rhs.getToStop(), rhs.getToRoute(), toTrip, true);

    if (fromPoint == null || toPoint == null) {
      issueStore.add(new InvalidGtfsTransfer("fromPoint / toPoint doesn't exist", rhs));
      return null;
    }

    return new ConstrainedTransfer(null, fromPoint, toPoint, constraint);
  }

  private void setup(boolean run) {
    if (!run) {
      return;
    }

    for (Trip trip : tripMapper.getMappedTrips()) {
      tripsByRoute.put(trip.getRoute(), trip);
    }
  }

  private TransferConstraint mapConstraint(Transfer rhs) {
    var builder = TransferConstraint.of();

    builder.guaranteed(rhs.getTransferType() == GUARANTEED);

    // A transfer is stay seated, if it is either explicitly mapped as such, or in the same block
    // and not explicitly disallowed.
    builder.staySeated(rhs.getTransferType() == STAY_SEATED);

    builder.priority(mapTypeToPriority(rhs.getTransferType()));

    if (!discardMinTransferTimes && rhs.isMinTransferTimeSet()) {
      builder.minTransferTime(rhs.getMinTransferTime());
    }

    return builder.build();
  }

  private TransferPoint mapTransferPoint(
    org.onebusaway.gtfs.model.Stop rhsStopOrStation,
    org.onebusaway.gtfs.model.Route rhsRoute,
    Trip trip,
    boolean boardTrip
  ) {
    Route route = routeMapper.map(rhsRoute);
    Station station = null;
    RegularStop stop = null;

    // A transfer is specified using Stops and/or Station, according to the GTFS specification:
    //
    //    If the stop ID refers to a station that contains multiple stops, this transfer rule
    //    applies to all stops in that station.
    //
    // Source: https://developers.google.com/transit/gtfs/reference/transfers-file

    if (rhsStopOrStation.getLocationType() == 0) {
      stop = stopMapper.map(rhsStopOrStation);
    } else {
      station = stationMapper.map(rhsStopOrStation);
    }
    if (trip != null) {
      // A trip may visit the same stop twice, but we ignore that and only add the first stop
      // we find. Pattern that start and end at the same stop is supported.
      int stopPositionInPattern;
      if (boardTrip) {
        stopPositionInPattern = boardStopPosition(trip, stop, station);
      } else {
        stopPositionInPattern = alightStopPosition(trip, stop, station);
      }
      return stopPositionInPattern < 0 ? null : new TripTransferPoint(trip, stopPositionInPattern);
    } else if (route != null) {
      if (stop != null) {
        return new RouteStopTransferPoint(route, stop);
      } else if (station != null) {
        return new RouteStationTransferPoint(route, station);
      }
    } else if (stop != null) {
      return new StopTransferPoint(stop);
    } else if (station != null) {
      return new StationTransferPoint(station);
    }

    throw new IllegalStateException("Should not get here!");
  }

  private int boardStopPosition(Trip trip, RegularStop stop, Station station) {
    List<StopTime> stopTimes = stopTimesByTrip.get(trip);

    Predicate<StopLocation> stopMatches = station != null
      ? s -> ((s instanceof RegularStop regStop) && regStop.getParentStation() == station)
      : s -> s == stop;

    for (int i = 0; i < stopTimes.size() - 1; i++) {
      StopTime stopTime = stopTimes.get(i);
      if (stopTime.getPickupType().isNotRoutable()) {
        continue;
      }

      if (stopMatches.test(stopTime.getStop())) {
        return i;
      }
    }
    return -1;
  }

  private int alightStopPosition(Trip trip, RegularStop stop, Station station) {
    List<StopTime> stopTimes = stopTimesByTrip.get(trip);

    Predicate<StopLocation> stopMatches = station != null
      ? s -> ((s instanceof RegularStop regStop) && regStop.getParentStation() == station)
      : s -> s == stop;

    for (int i = stopTimes.size() - 1; i > 0; i--) {
      StopTime stopTime = stopTimes.get(i);
      if (stopTime.getDropOffType().isNotRoutable()) {
        continue;
      }

      if (stopMatches.test(stopTime.getStop())) {
        return i;
      }
    }
    return -1;
  }
}
