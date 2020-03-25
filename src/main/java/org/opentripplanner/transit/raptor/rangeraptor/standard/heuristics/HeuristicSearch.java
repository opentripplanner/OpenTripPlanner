package org.opentripplanner.transit.raptor.rangeraptor.standard.heuristics;

import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.Heuristics;
import org.opentripplanner.transit.raptor.api.view.Worker;

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

    public boolean destinationReached() {
        return heuristics.destinationReached();
    }
}
