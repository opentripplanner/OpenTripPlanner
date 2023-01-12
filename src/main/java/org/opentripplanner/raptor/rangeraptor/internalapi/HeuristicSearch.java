package org.opentripplanner.raptor.rangeraptor.internalapi;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * Combine Heuristics and Worker into one class to be able to retrieve the heuristics after the
 * worker is invoked.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class HeuristicSearch<T extends RaptorTripSchedule> {

  private final RaptorWorker<T> raptorWorker;
  private final Heuristics heuristics;

  public HeuristicSearch(RaptorWorker<T> raptorWorker, Heuristics heuristics) {
    this.raptorWorker = raptorWorker;
    this.heuristics = heuristics;
  }

  public Heuristics heuristics() {
    return heuristics;
  }

  public void route() {
    raptorWorker.route();
  }

  public boolean destinationReached() {
    return heuristics.destinationReached();
  }
}
