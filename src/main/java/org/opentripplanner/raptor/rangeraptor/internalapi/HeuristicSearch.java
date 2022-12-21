package org.opentripplanner.raptor.rangeraptor.internalapi;

import java.util.Collection;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.response.StopArrivals;

/**
 * Combine Heuristics and Worker into one class to be able to retrieve the heuristics after the
 * worker is invoked.
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
  public void route() {
    worker.route();
  }

  @Override
  public Collection<RaptorPath<T>> paths() {
    return worker.paths();
  }

  @Override
  public StopArrivals stopArrivals() {
    return worker.stopArrivals();
  }

  public boolean destinationReached() {
    return heuristics.destinationReached();
  }
}
