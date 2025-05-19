package org.opentripplanner.api.resource;

/**
 * Holds information to be included in the response for debugging and profiling purposes.
 */
public class DebugOutput {

  /**
   * Time taken for worker initialization in nanoseconds.
   */
  public final long precalculationTime;

  /**
   * Time taken in the direct street router in nanoseconds.
   */
  public final long directStreetRouterTime;

  /**
   * Time taken in the transit router (including access/egress street router) in nanoseconds.
   * Detailed timing information within the transit router is sored in transitRouterTimes.
   *
   * @see DebugOutput#transitRouterTimes;
   */
  public final long transitRouterTime;

  /**
   * Time taken in sorting and filtering of the itineraries from the direct and transit routers in
   * nanoseconds.
   */
  public final long filteringTime;

  /**
   * Time taken for the mapping of the internal classes to the api classes in nanoseconds.
   */
  public final long renderingTime;

  /**
   * Total time taken for the route request in nanoseconds.
   */
  public final long totalTime;

  /**
   * Detailed timing information of within the transit router.
   */
  public final TransitTimingOutput transitRouterTimes;

  public DebugOutput(
    long precalculationTime,
    long directStreetRouterTime,
    long transitRouterTime,
    long filteringTime,
    long renderingTime,
    long totalTime,
    TransitTimingOutput transitRouterTimes
  ) {
    this.precalculationTime = precalculationTime;
    this.directStreetRouterTime = directStreetRouterTime;
    this.transitRouterTime = transitRouterTime;
    this.filteringTime = filteringTime;
    this.renderingTime = renderingTime;
    this.totalTime = totalTime;
    this.transitRouterTimes = transitRouterTimes;
  }
}
