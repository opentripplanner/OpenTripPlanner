package org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer;

import java.util.function.IntFunction;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.StopIndexForRaptor;


/**
 * This class generate TransferPoints adapted to Raptor. The internal model
 * {@link org.opentripplanner.model.transfer.TransferPoint} can not be used by Raptor as is, so
 * we transform them into {@link TransferPointMatcher}. For example to speed ut the search in
 * Raptor we avoid fetching Stops from memory and instead uses a {@code stopIndex}. This index is
 * not necessarily fixed, but generated for the
 * {@link org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer}, so we need to
 * generate
 */


final class TransferPointForPatternFactory {

    /** private constructor to prevent utility class from instantiation */
    private TransferPointForPatternFactory() { /* empty */ }

    static TransferPointMatcher createTransferPointForPattern(
            Station station,
            StopIndexForRaptor stopIndex
    ) {
        return new StationSP(stopIndex::stopByIndex, station);
    }

    static TransferPointMatcher createTransferPointForPattern(int stopIndex) {
        return new StopSP(stopIndex);
    }

    static TransferPointMatcher createTransferPointForPattern(Route route, int sourceStopIndex) {
        return new RouteSP(route, sourceStopIndex);
    }

    static TransferPointMatcher createTransferPointForPattern(Trip trip, int sourceStopIndex) {
        return new TripSP(trip, sourceStopIndex);
    }

    private static class StationSP implements TransferPointMatcher {

        // This is potentially slow, can be replaced with a set of stopIndexes for the
        // station to improve performance - not tested
        private final IntFunction<StopLocation> toStop;
        private final Station station;

        private StationSP(IntFunction<StopLocation> toStop, Station station) {
            this.toStop = toStop;
            this.station = station;
        }

        @Override
        public boolean match(int stopIndex, Trip trip) {
            return station == toStop.apply(stopIndex).getParentStation();
        }
    }

    private static class StopSP implements TransferPointMatcher {

        private final int stopIndex;

        private StopSP(int stopIndex) {
            this.stopIndex = stopIndex;
        }

        @Override
        public boolean match(int stopIndex, Trip trip) {
            return this.stopIndex == stopIndex;
        }
    }

    private static class RouteSP implements TransferPointMatcher {

        private final Route route;
        private final int stopIndex;

        private RouteSP(Route route, int stopIndex) {
            this.route = route;
            this.stopIndex = stopIndex;
        }

        @Override
        public boolean match(int stopIndex, Trip trip) {
            return this.stopIndex == stopIndex && this.route == trip.getRoute();
        }
    }

    private static class TripSP implements TransferPointMatcher {

        private final Trip trip;
        private final int stopIndex;

        private TripSP(Trip trip, int stopIndex) {
            this.trip = trip;
            this.stopIndex = stopIndex;
        }

        @Override
        public boolean match(int stopIndex, Trip trip) {
            return this.stopIndex == stopIndex && this.trip == trip;
        }
    }
}
