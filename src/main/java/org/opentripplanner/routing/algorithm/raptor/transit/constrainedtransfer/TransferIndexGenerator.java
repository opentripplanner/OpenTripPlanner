package org.opentripplanner.routing.algorithm.raptor.transit.constrainedtransfer;

import static org.opentripplanner.routing.algorithm.raptor.transit.constrainedtransfer.TransferPointForPatternFactory.createTransferPointForPattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.RouteTransferPoint;
import org.opentripplanner.model.transfer.StationTransferPoint;
import org.opentripplanner.model.transfer.StopTransferPoint;
import org.opentripplanner.model.transfer.TransferPoint;
import org.opentripplanner.model.transfer.TripTransferPoint;
import org.opentripplanner.routing.algorithm.raptor.transit.StopIndexForRaptor;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternWithRaptorStopIndexes;

public class TransferIndexGenerator {

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

            for (var fromPoint : findTPoints(tx.getFrom())) {
                if (fromPoint.canAlight()) {
                    for (var toPoint : findTPoints(tx.getTo())) {
                        if (toPoint.canBoard() && !fromPoint.equals(toPoint)) {
                            fromPoint.addTransferConstraints(tx, toPoint);
                        }
                    }
                }
            }
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

    private Collection<TPoint> findTPoints(TransferPoint txPoint) {
        if (txPoint.isStationTransferPoint()) {
            return findTPoints(txPoint.asStationTransferPoint());
        }
        else if (txPoint.isStopTransferPoint()) {
            return findTPoints(txPoint.asStopTransferPoint());
        }
        else if (txPoint.isRouteTransferPoint()) {
            return findTPoint(txPoint.asRouteTransferPoint());
        }
        else {
            return findTPoints(txPoint.asTripTransferPoint());
        }
    }

    private List<TPoint> findTPoints(StationTransferPoint point) {
        var station = point.getStation();
        var patterns = patternsByStation.get(station);
        var sourcePoint = createTransferPointForPattern(station, stopIndex);
        var result = new ArrayList<TPoint>();

        for (TripPatternWithRaptorStopIndexes pattern : patterns) {
            var stops = pattern.getPattern().getStopPattern().getStops();
            for (int pos = 0; pos < stops.length; ++pos) {
                if (point.getStation() == stops[pos].getParentStation()) {
                    result.add(new TPoint(pattern, sourcePoint, null, pos));
                }
            }
        }
        return result;
    }

    private List<TPoint> findTPoints(StopTransferPoint point) {
        var stop = point.asStopTransferPoint().getStop();
        var patterns = patternsByStop.get(stop);
        var sourcePoint = createTransferPointForPattern(stopIndex.indexOf(stop));

        var result = new ArrayList<TPoint>();
        for (TripPatternWithRaptorStopIndexes pattern : patterns) {
            var stops = pattern.getPattern().getStopPattern().getStops();
            for (int pos = 0; pos < stops.length; ++pos) {
                if (point.getStop() == stops[pos]) {
                    result.add(new TPoint(pattern, sourcePoint, null, pos));
                }
            }
        }
        return result;
    }

    private List<TPoint> findTPoint(RouteTransferPoint point) {
        var route = point.getRoute();
        var patterns = patternsByRoute.get(route);
        int stopPosInPattern = point.getStopPositionInPattern();
        int stopIndex = patterns.get(0).stopIndex(stopPosInPattern);
        var sourcePoint = createTransferPointForPattern(route, stopIndex);
        return patterns.stream()
                .map(p -> new TPoint(p, sourcePoint, null, stopPosInPattern))
                .collect(Collectors.toList());
    }

    private List<TPoint> findTPoints(TripTransferPoint point) {
        var trip = point.getTrip();
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
            int lastStopPosition = pattern.getPattern().getStopPattern().getSize() - 1;
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
