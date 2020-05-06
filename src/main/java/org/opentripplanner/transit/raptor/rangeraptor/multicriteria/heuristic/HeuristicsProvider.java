package org.opentripplanner.transit.raptor.rangeraptor.multicriteria.heuristic;

import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.Heuristics;
import org.opentripplanner.transit.raptor.rangeraptor.RoundProvider;
import org.opentripplanner.transit.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals.AbstractStopArrival;
import org.opentripplanner.transit.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;

/**
 * A wrapper around {@link Heuristics} to cash elements to avoid recalculation
 * of heuristic properties.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class HeuristicsProvider<T extends RaptorTripSchedule> {
    private final Heuristics heuristics;
    private final RoundProvider roundProvider;
    private final DestinationArrivalPaths<T> paths;
    private final CostCalculator costCalculator;
    private final HeuristicAtStop[] stops;
    private final DebugHandlerFactory<T> debugHandlerFactory;


    public HeuristicsProvider() {
        this(null, null, null, null, null);
    }

    public HeuristicsProvider(
            Heuristics heuristics,
            RoundProvider roundProvider,
            DestinationArrivalPaths<T> paths,
            CostCalculator costCalculator,
            DebugHandlerFactory<T> debugHandlerFactory
    ) {
        this.heuristics = heuristics;
        this.roundProvider = roundProvider;
        this.costCalculator = costCalculator;
        this.paths = paths;
        this.stops = heuristics == null ? null : new HeuristicAtStop[heuristics.size()];
        this.debugHandlerFactory = debugHandlerFactory;
    }

    /**
     * This is a very effective optimization, enabled by the
     * {@link org.opentripplanner.transit.raptor.api.request.Optimization#PARETO_CHECK_AGAINST_DESTINATION}.
     */
    public boolean rejectDestinationArrivalBasedOnHeuristic(AbstractStopArrival<T> arrival) {
        if(heuristics == null || paths.isEmpty()) {
            return false;
        }
        boolean rejected = !qualify(arrival.stop(), arrival.arrivalTime(), arrival.travelDuration(), arrival.cost());

        if(rejected) {
            debugRejectByOptimization(arrival);
        }
        return rejected;
    }


    /* private methods */

    private void debugRejectByOptimization(AbstractStopArrival<T> arrival) {
        if (debugHandlerFactory.isDebugStopArrival(arrival.stop())) {
            String details = rejectErrorMessage(arrival.stop()) +
                    ", Existing paths: " + paths;

            debugHandlerFactory.debugStopArrival().reject(
                    arrival,
                    null,
                    "The element is rejected because the destination is not reachable within the limit " +
                            "based on heuristic. Details: " + details
            );
        }
    }

    /**
     * This is used to make an optimistic guess for the best possible arrival at the destination,
     * using the given arrival and a pre-calculated heuristics.
     */
    private boolean qualify(int stop, int arrivalTime, int travelDuration, int cost) {
        HeuristicAtStop h = get(stop);

        if(h == null) {
            return false;
        }
        int minArrivalTime = arrivalTime + h.getMinTravelDuration();
        int minNumberOfTransfers = roundProvider.round() - 1 + h.getMinNumTransfers();
        int minTravelDuration = travelDuration + h.getMinTravelDuration();
        int minCost = cost + h.getMinCost();
        int departureTime = minArrivalTime - minTravelDuration;
        return paths.qualify(departureTime, minArrivalTime, minNumberOfTransfers, minCost);
    }

    private String rejectErrorMessage(int stop) {
        return get(stop) == null
                ? "The stop was not reached in the heuristic calculation."
                : get(stop).toString();

    }

    private HeuristicAtStop get(int stop) {
        if(stops[stop] == null && heuristics.reached(stop)) {
            stops[stop] = createHeuristicAtStop(
                    heuristics.bestTravelDuration(stop),
                    heuristics.bestNumOfTransfers(stop)
            );
        }
        return stops[stop];
    }

    private HeuristicAtStop createHeuristicAtStop(int bestTravelDuration, int bestNumOfTransfers) {
        return new HeuristicAtStop(
                bestTravelDuration,
                bestNumOfTransfers,
                costCalculator.calculateMinCost(bestTravelDuration, bestNumOfTransfers)
        );
    }
}
