package org.opentripplanner.raptor.api.response;

import java.util.Collection;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This is the result of a raptor search including the result paths, the original request
 * (unmodified) and the main request used to perform the raptor search. The {@link RaptorService}
 * might perform additional heuristic searches, too, but the requests for these are not returned.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class RaptorResponse<T extends RaptorTripSchedule> {

  private final Collection<RaptorPath<T>> paths;
  private final RaptorRequest<T> requestUsed;
  private final StopArrivals arrivals;
  private final boolean heuristicPathExist;

  public RaptorResponse(
    Collection<RaptorPath<T>> paths,
    StopArrivals arrivals,
    RaptorRequest<T> requestUsed,
    boolean heuristicPathExist
  ) {
    this.paths = paths;
    this.arrivals = arrivals;
    this.requestUsed = requestUsed;
    this.heuristicPathExist = heuristicPathExist;
  }

  /**
   * The result paths found in the search.
   */
  public Collection<RaptorPath<T>> paths() {
    return paths;
  }

  public boolean containsUnknownPaths() {
    return paths.stream().anyMatch(RaptorPath::isUnknownPath);
  }

  /**
   * The end state of the search, with arrival times and lowest number of transfers. If multiple
   * routing workers are called, the main worker result is returned.
   */
  public StopArrivals getArrivals() {
    return arrivals;
  }

  /**
   * The actual request used to perform the travel search. In the case of a multi-criteria search,
   * heuristics is used to optimize the search and the request is changed to account for this. Also,
   * different optimization may add filters (stop filter) to the request. Heuristics is also used to
   * "guess" on an appropriate search-window, earliest-departure-time and latest-arrival-time.
   */
  public RaptorRequest<T> requestUsed() {
    return requestUsed;
  }

  /**
   * Return {@code true} if the heuristic and the main search does not find any connections.
   * Searching again with another time/search-window will not produce any results. There is no paths
   * in the set of days provided in the transit data with the request usd.
   */
  public boolean noConnectionFound() {
    return paths.isEmpty() && !heuristicPathExist;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(RaptorResponse.class)
      .addObj("paths", paths)
      .addObj("requestUsed", requestUsed)
      .toString();
  }
}
