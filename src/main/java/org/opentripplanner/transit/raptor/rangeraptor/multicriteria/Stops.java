package org.opentripplanner.transit.raptor.rangeraptor.multicriteria;


import org.opentripplanner.transit.raptor.api.debug.DebugLogger;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals.AbstractStopArrival;
import org.opentripplanner.transit.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.util.BitSetIterator;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;


/**
 * This class serve as a wrapper for all stop arrival pareto set, one set for each stop.
 * It also keep track of stops visited since "last mark".
 * <p>
 *
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class Stops<T extends RaptorTripSchedule> {
    private final StopArrivalParetoSet<T>[] stops;
    private final BitSet touchedStops;
    private final DebugHandlerFactory<T> debugHandlerFactory;
    private final DebugStopArrivalsStatistics debugStats;

    /**
     * Set the time at a transit index iff it is optimal. This sets both the best time and the transfer time
     */
    public Stops(
            int nStops,
            Collection<RaptorTransfer> egressLegs,
            DestinationArrivalPaths<T> paths,
            CostCalculator costCalculator,
            DebugHandlerFactory<T> debugHandlerFactory,
            DebugLogger debugLogger
    ) {
        //noinspection unchecked
        this.stops = (StopArrivalParetoSet<T>[]) new StopArrivalParetoSet[nStops];
        this.touchedStops = new BitSet(nStops);
        this.debugHandlerFactory = debugHandlerFactory;
        this.debugStats = new DebugStopArrivalsStatistics(debugLogger);

        Collection<Map.Entry<Integer, List<RaptorTransfer>>> groupedEgressLegs = egressLegs
            .stream()
            .collect(Collectors.groupingBy(RaptorTransfer::stop))
            .entrySet();

        for (Map.Entry<Integer, List<RaptorTransfer>> it : groupedEgressLegs) {
            glueTogetherEgressStopWithDestinationArrivals(it, costCalculator, paths);
        }
    }

    boolean updateExist() {
        return !touchedStops.isEmpty();
    }

    IntIterator stopsTouchedIterator() {
        return new BitSetIterator(touchedStops);
    }

    void addStopArrival(AbstractStopArrival<T> arrival) {
        boolean added = findOrCreateSet(arrival.stop()).add(arrival);
        if (added) {
            touchedStops.set(arrival.stop());
        }
    }

    void debugStateInfo() {
        debugStats.debugStatInfo(stops);
    }

    /** List all transits arrived this round. */
    Iterable<AbstractStopArrival<T>> listArrivalsAfterMarker(final int stop) {
        StopArrivalParetoSet<T> it = stops[stop];
        if(it==null) {
            return emptyList();
        }
        return it.elementsAfterMarker();
    }

    void clearTouchedStopsAndSetStopMarkers() {
        IntIterator it = stopsTouchedIterator();
        while (it.hasNext()) {
            stops[it.next()].markAtEndOfSet();
        }
        touchedStops.clear();
    }


    /* private methods */

    private StopArrivalParetoSet<T> findOrCreateSet(final int stop) {
        if(stops[stop] == null) {
            stops[stop] = StopArrivalParetoSet.createStopArrivalSet(stop, debugHandlerFactory);
        }
        return stops[stop];
    }

    /**
     * This method creates a ParetoSet for the given egress stop. When arrivals are added to the
     * stop, the "glue" make sure new destination arrivals is added to the destination arrivals.
     */
    private void glueTogetherEgressStopWithDestinationArrivals(
            Map.Entry<Integer, List<RaptorTransfer>> egressLegs,
            CostCalculator costCalculator,
            DestinationArrivalPaths<T> paths
    ) {
        int stop = egressLegs.getKey();
        // The factory is creating the actual "glue"
        this.stops[stop] = StopArrivalParetoSet.createEgressStopArrivalSet(
                egressLegs,
                costCalculator,
                paths,
                debugHandlerFactory
        );
    }
}
