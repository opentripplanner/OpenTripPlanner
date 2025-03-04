package org.opentripplanner.model.transfer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * A map from any TransferPoint to an instances of type E. This is used to look up entities by trip
 * and stop. The {@link TransferPoint} class only plays a role when the map is created.
 */
class TransferPointMap<E> {

  private final Map<TripKey, E> tripMap = new HashMap<>();
  private final Map<RouteStopKey, E> routeStopMap = new HashMap<>();
  private final Map<RouteStationKey, E> routeStationMap = new HashMap<>();
  private final Map<StopLocation, E> stopMap = new HashMap<>();
  private final Map<Station, E> stationMap = new HashMap<>();

  void put(TransferPoint point, E e) {
    if (point.isTripTransferPoint()) {
      var tp = point.asTripTransferPoint();
      tripMap.put(new TripKey(tp.getTrip(), tp.getStopPositionInPattern()), e);
    } else if (point.isRouteStopTransferPoint()) {
      var rp = point.asRouteStopTransferPoint();
      routeStopMap.put(new RouteStopKey(rp.getRoute(), rp.getStop()), e);
    } else if (point.isRouteStationTransferPoint()) {
      var rp = point.asRouteStationTransferPoint();
      routeStationMap.put(new RouteStationKey(rp.getRoute(), rp.getStation()), e);
    } else if (point.isStopTransferPoint()) {
      stopMap.put(point.asStopTransferPoint().getStop(), e);
    } else if (point.isStationTransferPoint()) {
      stationMap.put(point.asStationTransferPoint().getStation(), e);
    } else {
      throw new IllegalArgumentException("Unknown TransferPoint type: " + point);
    }
  }

  E computeIfAbsent(TransferPoint point, Supplier<E> creator) {
    if (point.isTripTransferPoint()) {
      var tp = point.asTripTransferPoint();
      return tripMap.computeIfAbsent(new TripKey(tp.getTrip(), tp.getStopPositionInPattern()), k ->
        creator.get()
      );
    } else if (point.isRouteStopTransferPoint()) {
      var rp = point.asRouteStopTransferPoint();
      return routeStopMap.computeIfAbsent(new RouteStopKey(rp.getRoute(), rp.getStop()), k ->
        creator.get()
      );
    } else if (point.isRouteStationTransferPoint()) {
      var rp = point.asRouteStationTransferPoint();
      return routeStationMap.computeIfAbsent(
        new RouteStationKey(rp.getRoute(), rp.getStation()),
        k -> creator.get()
      );
    } else if (point.isStopTransferPoint()) {
      var sp = point.asStopTransferPoint();
      return stopMap.computeIfAbsent(sp.getStop(), k -> creator.get());
    } else if (point.isStationTransferPoint()) {
      var sp = point.asStationTransferPoint();
      return stationMap.computeIfAbsent(sp.getStation(), k -> creator.get());
    }
    throw new IllegalArgumentException("Unknown TransferPoint type: " + point);
  }

  /**
   * List all elements which matches any of the transfer points added to the map.
   */
  List<E> get(Trip trip, StopLocation stop, int stopPointInPattern) {
    return Stream.of(
      tripMap.get(new TripKey(trip, stopPointInPattern)),
      routeStopMap.get(new RouteStopKey(trip.getRoute(), stop)),
      routeStationMap.get(new RouteStationKey(trip.getRoute(), stop.getParentStation())),
      stopMap.get(stop),
      stationMap.get(stop.getParentStation())
    )
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  private record TripKey(Trip trip, int stopPositionInPattern) {}

  private record RouteStopKey(Route route, StopLocation stop) {}

  private record RouteStationKey(Route route, Station station) {}
}
