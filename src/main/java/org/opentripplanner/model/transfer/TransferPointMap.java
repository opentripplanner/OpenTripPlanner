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
    private final Map<T2<Route, Integer>, E> routeMap = new HashMap<>();
    private final Map<StopLocation, E> stopMap = new HashMap<>();
    private final Map<Station, E> stationMap = new HashMap<>();

    void put(TransferPoint point, E e) {
        if(point.isTripTransferPoint()) {
            var tp = point.asTripTransferPoint();
            tripMap.put(tripKey(tp.getTrip(), tp.getStopPositionInPattern()), e);
        }
        else if(point.isRouteTransferPoint()) {
            var rp = point.asRouteTransferPoint();
            routeMap.put(routeKey(rp.getRoute(), rp.getStopPositionInPattern()), e);
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
        else if(point.isRouteTransferPoint()) {
            var rp = point.asRouteTransferPoint();
            return routeMap.computeIfAbsent(routeKey(rp.getRoute(), rp.getStopPositionInPattern()), k -> creator.get());
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
        return Stream.of(
                tripMap.get(tripKey(trip, stopPointInPattern)),
                routeMap.get(routeKey(trip.getRoute(), stopPointInPattern)),
                stopMap.get(stop),
                stationMap.get(stop.getParentStation())
        )
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static T2<Trip, Integer> tripKey(Trip trip, int stopPositionInPattern) {
        return new T2<>(trip, stopPositionInPattern);
    }

    private static T2<Route, Integer> routeKey(Route route, int stopPositionInPattern) {
        return new T2<>(route, stopPositionInPattern);
    }
}
