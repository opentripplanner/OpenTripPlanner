package org.opentripplanner.transit.raptor.rangeraptor.standard.heuristics;

import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.Heuristics;
import org.opentripplanner.transit.raptor.api.view.Worker;

import java.util.BitSet;
import java.util.Collection;

/**
 * Combine Heuristics and Worker into one class to be able to retrieve the
 * heuristics after the worker is invoked.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class HeuristicSearch<T extends RaptorTripSchedule> implements Worker<T> {
    private final Worker<T> worker;
    private final Heuristics heuristics;

    public HeuristicSearch(Worker<T> worker, Heuristics heuristics) {
        this.worker = worker;
        this.heuristics = heuristics;
    }

    public Heuristics heuristics() {
        return heuristics;
    }

    @Override
    public Collection<Path<T>> route() {
        return worker.route();
    }


    /**
     * Combine two Heuristics to produce a stop filter. The heuristics should be computed with a
     * forward and a reverse search. For a given stop the two {@link Heuristics#bestNumOfTransfers(int)}
     * are added and if the sum is better than the given maxNumberOfTransferLimit, the flag in
     * the returned bit set is enabled.
     */
    public BitSet stopFilter(HeuristicSearch<T> other, final int numberOfAdditionalTransfers) {
        int maxNumberOfTransferLimit = numberOfAdditionalTransfers + heuristics.bestOverallJourneyNumOfTransfers();
        Heuristics h2 = other.heuristics();
        int n = heuristics.size();
        BitSet stopFilter = new BitSet(n);
        for (int i=0; i<n; ++i) {
            // We add 1 extra transfer because you need to get off forward search and on reverse search
            // at the given stop. If you are on the same vehicle you do not transfer at this stop.
            int totNTransfers = heuristics.bestNumOfTransfers(i) + h2.bestNumOfTransfers(i) + 1;
            if (totNTransfers < maxNumberOfTransferLimit) {
                stopFilter.set(i, true);
            }
        }
        return stopFilter;
    }

    public boolean destinationReached() {
        return heuristics.destinationReached();
    }
}
