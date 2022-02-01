package org.opentripplanner.routing.algorithm.raptor.transit.constrainedtransfer;

import static org.opentripplanner.routing.algorithm.raptor.transit.constrainedtransfer.TransferPointForPatternFactory.createTransferPointForPattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.RouteStationTransferPoint;
import org.opentripplanner.model.transfer.RouteStopTransferPoint;
import org.opentripplanner.model.transfer.StationTransferPoint;
import org.opentripplanner.model.transfer.StopTransferPoint;
import org.opentripplanner.model.transfer.TransferPoint;
import org.opentripplanner.model.transfer.TripTransferPoint;
import org.opentripplanner.routing.algorithm.raptor.transit.StopIndexForRaptor;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternWithRaptorStopIndexes;

public class TransferIndexGenerator {
    private static final boolean BOARD = true;
    private static final boolean ALIGHT = false;

    private final Collection<ConstrainedTransfer> constrainedTransfers;
    private final Map<Station, List<TripPatternWithRaptorStopIndexes>> patternsByStation = new HashMap<>();
    private final Map<StopLocation, List<TripPatternWithRaptorStopIndexes>> patternsByStop = new HashMap<>();
    private final Map<Route, List<TripPatternWithRaptorStopIndexes>> patternsByRoute = new HashMap<>();
    private final Map<Trip, List<TripPatternWithRaptorStopIndexes>> patternsByTrip = new HashMap<>();
    private final StopIndexForRaptor stopIndex;

    public TransferIndexGenerator(
            Collection<ConstrainedTransfer> constrainedTransfers,
            Collection<TripPatternWithRaptorStopIndexes> tripPatterns,
            StopIndexForRaptor stopIndex
    ) {
        this.constrainedTransfers = constrainedTransfers;
        this.stopIndex = stopIndex;
        setupPatternByTripIndex(tripPatterns);
    }

    public void generateTransfers() {
        for (ConstrainedTransfer tx : constrainedTransfers) {
            var c = tx.getTransferConstraint();
            // Only add transfers witch have an effect on the Raptor routing here.
            // Some transfers only have the priority set, and that is used in optimized-
            // transfers, but not in Raptor.
            if (!c.useInRaptorRouting()) { continue; }

            findTPoints(tx.getFrom(), ALIGHT).stream()
                    .filter(TPoint::canAlight)
                    .forEachOrdered(fromPoint -> {
                        for (var toPoint : findTPoints(tx.getTo(), BOARD)) {
                            if (toPoint.canBoard() && !fromPoint.equals(toPoint)) {
                                fromPoint.addTransferConstraints(tx, toPoint);
                            }
                        }
                    });
        }
        sortAllTransfersByRanking();
    }

    private void sortAllTransfersByRanking() {
        for (var patterns : patternsByRoute.values()) {
            for (var pattern : patterns) {
                pattern.sortConstrainedTransfers();
            }
        }
    }

    private void setupPatternByTripIndex(Collection<TripPatternWithRaptorStopIndexes> tripPatterns) {
        for (TripPatternWithRaptorStopIndexes pattern : tripPatterns) {
            TripPattern tripPattern = pattern.getPattern();

            patternsByRoute
                    .computeIfAbsent(tripPattern.getRoute(), t -> new ArrayList<>())
                    .add(pattern);

            for (Trip trip : tripPattern.getTrips()) {
                patternsByTrip.computeIfAbsent(trip, t -> new ArrayList<>()).add(pattern);
            }

            for (StopLocation stop : tripPattern.getStops()) {
                patternsByStop.computeIfAbsent(stop, t -> new ArrayList<>()).add(pattern);
                Station station = stop.getParentStation();
                if (station != null) {
                    patternsByStation.computeIfAbsent(station, t -> new ArrayList<>()).add(pattern);
                }
            }
        }
    }

    private Collection<TPoint> findTPoints(TransferPoint txPoint, boolean boarding) {
        if (txPoint.isStationTransferPoint()) {
            return findTPoints(txPoint.asStationTransferPoint());
        }
        else if (txPoint.isStopTransferPoint()) {
            return findTPoints(txPoint.asStopTransferPoint());
        }
        else if (txPoint.isRouteStationTransferPoint()) {
            return findTPoint(txPoint.asRouteStationTransferPoint(), boarding);
        }
        else if (txPoint.isRouteStopTransferPoint()) {
            return findTPoint(txPoint.asRouteStopTransferPoint(), boarding);
        }
        else {
            return findTPoints(txPoint.asTripTransferPoint());
        }
    }

    private List<TPoint> findTPoints(StationTransferPoint point) {
        var station = point.getStation();
        var patterns = patternsByStation.get(station);

        if(patterns == null) { return List.of(); }

        var sourcePoint = createTransferPointForPattern(station, stopIndex);
        var result = new ArrayList<TPoint>();

        for (TripPatternWithRaptorStopIndexes pattern : patterns) {
            var tripPattern = pattern.getPattern();
            for (int pos = 0; pos < tripPattern.numberOfStops(); ++pos) {
                if (point.getStation() == tripPattern.getStop(pos).getParentStation()) {
                    result.add(new TPoint(pattern, sourcePoint, null, pos));
                }
            }
        }
        return result;
    }

    private List<TPoint> findTPoints(StopTransferPoint point) {
        var stop = point.asStopTransferPoint().getStop();
        var patterns = patternsByStop.get(stop);

        if(patterns == null) { return List.of(); }

        var sourcePoint = createTransferPointForPattern(stopIndex.indexOf(stop));
        var result = new ArrayList<TPoint>();

        for (TripPatternWithRaptorStopIndexes pattern : patterns) {
            var p = pattern.getPattern();
            for (int pos = 0; pos < p.numberOfStops(); ++pos) {
                if (point.getStop() == p.getStop(pos)) {
                    result.add(new TPoint(pattern, sourcePoint, null, pos));
                }
            }
        }
        return result;
    }

    private List<TPoint> findTPoint(RouteStationTransferPoint point, boolean boarding) {
        return findTPointForRoute(
                point.getRoute(),
                boarding ? p -> p.findBoardingStopPositionInPattern(point.getStation())
                         : p -> p.findAlightStopPositionInPattern(point.getStation())
        );
    }

    private List<TPoint> findTPoint(RouteStopTransferPoint point, boolean boarding) {
        return findTPointForRoute(
                point.getRoute(),
                boarding ? p -> p.findBoardingStopPositionInPattern(point.getStop())
                         : p -> p.findAlightStopPositionInPattern(point.getStop())
        );
    }

    private List<TPoint> findTPointForRoute(
            Route route,
            ToIntFunction<TripPattern> resolveStopPosInPattern
    ) {
        var patterns = patternsByRoute.get(route);

        // A route should have a pattern(trip), but it does not hurt to check here
        if(patterns == null) { return List.of(); }

        var points = new ArrayList<TPoint>();

        for (var pattern : patterns) {
            int stopPosInPattern = resolveStopPosInPattern.applyAsInt(pattern.getPattern());
            int stopIndex = pattern.stopIndex(stopPosInPattern);
            var sourcePoint = createTransferPointForPattern(route, stopIndex);
            points.add(new TPoint(pattern, sourcePoint, null, stopPosInPattern));
        }
        return points;
    }

    private List<TPoint> findTPoints(TripTransferPoint point) {
        var trip = point.getTrip();
        // All trips have at least one pattern, no need to chech for null here
        var patterns = patternsByTrip.get(trip);
        int stopPosInPattern = point.getStopPositionInPattern();
        int stopIndex = patterns.get(0).stopIndex(stopPosInPattern);
        var sourcePoint = createTransferPointForPattern(trip, stopIndex);
        return patterns.stream()
                .map(p -> new TPoint(p, sourcePoint, trip, stopPosInPattern))
                .collect(Collectors.toList());
    }

    private static class TPoint {
        TripPatternWithRaptorStopIndexes pattern;
        TransferPointMatcher sourcePoint;
        Trip trip;
        int stopPosition;

        private TPoint(
                TripPatternWithRaptorStopIndexes pattern,
                TransferPointMatcher sourcePoint,
                Trip trip,
                int stopPosition
        ) {
            this.pattern = pattern;
            this.sourcePoint = sourcePoint;
            this.trip = trip;
            this.stopPosition = stopPosition;
        }

        boolean canBoard() {
            // We prevent boarding at the last stop, this might be enforced by the
            // canBoard method, but we do not trust it here.
            int lastStopPosition = pattern.getPattern().numberOfStops() - 1;
            return stopPosition != lastStopPosition && pattern.getPattern().canBoard(stopPosition);
        }

        boolean canAlight() {
            // We prevent alighting at the first stop, this might be enforced by the
            // canAlight method, but we do not trust it here.
            return stopPosition != 0 && pattern.getPattern().canAlight(stopPosition);
        }

        void addTransferConstraints(ConstrainedTransfer tx, TPoint to) {
            int rank = tx.getSpecificityRanking();
            var c = tx.getTransferConstraint();

            // Forward search
            to.pattern.addTransferConstraintsForwardSearch(
                    to.stopPosition,
                    new TransferForPattern(sourcePoint, to.trip, rank, c)
            );
            // Reverse search
            pattern.addTransferConstraintsReverseSearch(
                    stopPosition,
                    new TransferForPattern(to.sourcePoint, trip, rank, c)
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {return true;}
            if (!(o instanceof TPoint)) {return false;}
            final TPoint tPoint = (TPoint) o;
            return stopPosition == tPoint.stopPosition
                    && Objects.equals(pattern, tPoint.pattern)
                    && Objects.equals(trip, tPoint.trip);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pattern, trip, stopPosition);
        }
    }
}
