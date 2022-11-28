package org.opentripplanner.raptor.api.debug;

/**
 * Implement this interface and pass it into Raptor to wrap the most important
 * steps in the algorithm. This make Raptor independent of the frameworks used and the
 * target monitoring system.
 */
public interface RaptorTimers {
  /**
   * This timer is used if no timer is set, and it does nothing.
   */
  RaptorTimers NOOP = new RaptorTimers() {
    @Override
    public void route(Runnable body) {
      body.run();
    }

    @Override
    public void findTransitForRound(Runnable body) {
      body.run();
    }

    @Override
    public void findTransfersForRound(Runnable body) {
      body.run();
    }

    @Override
    public RaptorTimers withNamePrefix(String namePrefix) {
      return this;
    }
  };

  /**
   * This method is called for each Range Raptor routing request. A Raptor search may first
   * do an arrival-time-criteria routing request, then do a reverse search and then a multi-criteria
   * routing request. In this case this method is called for all three requests.
   */
  void route(Runnable body);

  /**
   * This wrap finding all transit routes and updating the state with all transit-stop-arrivals
   * for a Range-Raptor round.
   */
  void findTransitForRound(Runnable body);

  /**
   * This wrap finding all transfers and updating the state with all transfer-stop-arrivals
   * for a Range-Raptor round.
   */
  void findTransfersForRound(Runnable body);

  /**
   * Create a new instance with a new name prefix. Useful when creating a new request for heuristic.
   */
  RaptorTimers withNamePrefix(String namePrefix);
}
