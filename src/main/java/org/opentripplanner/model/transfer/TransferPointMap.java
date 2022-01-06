package org.opentripplanner.model.transfer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Trip;

/**
 * A map from any TransferPoint to an instances of type E. This is used to look up
 * entities by trip and stop. The {@link TransferPoint} class only plays a role when the map is
 * created.
 */
class TransferPointMap<E> {
    private final Map<T2<Trip, Integer>, E> tripMap = new HashMap<>();
    private final Map<T2<Route, StopLocation>, E> routeStopMap = new HashMap<>();
    private final Map<T2<Route, Station>, E> routeStationMap = new HashMap<>();
    private final Map<StopLocation, E> stopMap = new HashMap<>();
    private final Map<Station, E> stationMap = new HashMap<>();

    void put(TransferPoint point, E e) {
        if(point.isTripTransferPoint()) {
            var tp = point.asTripTransferPoint();
            tripMap.put(tripKey(tp.getTrip(), tp.getStopPositionInPattern()), e);
        }
        else if(point.isRouteStopTransferPoint()) {
            var rp = point.asRouteStopTransferPoint();
            routeStopMap.put(routeStopKey(rp.getRoute(), rp.getStop()), e);
        }
        else if(point.isRouteStationTransferPoint()) {
            var rp = point.asRouteStationTransferPoint();
            routeStationMap.put(routeStationKey(rp.getRoute(), rp.getStation()), e);
        }
        else if(point.isStopTransferPoint()) {
            stopMap.put(point.asStopTransferPoint().getStop(), e);
        }
        else if(point.isStationTransferPoint()) {
            stationMap.put(point.asStationTransferPoint().getStation(), e);
        }
        else {
            throw new IllegalArgumentException("Unknown TransferPoint type: " + point);
        }
    }

    E computeIfAbsent(TransferPoint point, Supplier<E> creator) {
        if(point.isTripTransferPoint()) {
            var tp = point.asTripTransferPoint();
            return tripMap.computeIfAbsent(tripKey(tp.getTrip(), tp.getStopPositionInPattern()), k -> creator.get());
        }
        else if(point.isRouteStopTransferPoint()) {
            var rp = point.asRouteStopTransferPoint();
            return routeStopMap.computeIfAbsent(routeStopKey(rp.getRoute(), rp.getStop()), k -> creator.get());
        }
        else if(point.isRouteStationTransferPoint()) {
            var rp = point.asRouteStationTransferPoint();
            return routeStationMap.computeIfAbsent(routeStationKey(rp.getRoute(), rp.getStation()), k -> creator.get());
        }
        else if(point.isStopTransferPoint()) {
            var sp = point.asStopTransferPoint();
            return stopMap.computeIfAbsent(sp.getStop(), k -> creator.get());
        }
        else if(point.isStationTransferPoint()) {
            var sp = point.asStationTransferPoint();
            return stationMap.computeIfAbsent(sp.getStation(), k -> creator.get());
        }
        throw new IllegalArgumentException("Unknown TransferPoint type: " + point);
    }

    /**
     * List all elements witch matches any of the transfer points added to the map.
     */
    List<E> get(Trip trip, StopLocation stop, int stopPointInPattern) {
        var list = Stream.of(
                        tripMap.get(tripKey(trip, stopPointInPattern)),
                        routeStopMap.get(routeStopKey(trip.getRoute(), stop)),
                        routeStationMap.get(routeStationKey(trip.getRoute(), stop.getParentStation())),
                        stopMap.get(stop),
                        stationMap.get(stop.getParentStation())
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return list;
    }

    private static T2<Trip, Integer> tripKey(Trip trip, int stopPositionInPattern) {
        return new T2<>(trip, stopPositionInPattern);
    }

    private static T2<Route, StopLocation> routeStopKey(Route route, StopLocation stop) {
        return new T2<>(route, stop);
    }

    private static T2<Route, Station> routeStationKey(Route route, Station station) {
        return new T2<>(route, station);
    }
}
