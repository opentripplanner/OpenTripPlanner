package org.opentripplanner.raptor.api.response;

import java.util.Collection;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor.api.path.Path;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * This is the result of a raptor search including the the result paths, the original request
 * (unmodified) and the the main request used to perform the raptor search. The {@link
 * RaptorService} might perform additional heuristic searches,
 * too, but the requests for these are not returned.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class RaptorResponse<T extends RaptorTripSchedule> {

  private final Collection<Path<T>> paths;
  private final RaptorRequest<T> requestOriginal;
  private final RaptorRequest<T> requestUsed;
  private final StopArrivals arrivals;

  public RaptorResponse(
    Collection<Path<T>> paths,
    StopArrivals arrivals,
    RaptorRequest<T> requestOriginal,
    RaptorRequest<T> requestUsed
  ) {
    this.paths = paths;
    this.arrivals = arrivals;
    this.requestOriginal = requestOriginal;
    this.requestUsed = requestUsed;
  }

  /**
   * The result paths found in the search.
   */
  public Collection<Path<T>> paths() {
    return paths;
  }

  /**
   * The end state of the search, with arrival times and lowest number of transfers. If multiple
   * routing workers are called, the main worker result is returned.
   */
  public StopArrivals getArrivals() {
    return arrivals;
  }

  /**
   * The original request issued to perform the travel search.
   */
  public RaptorRequest<T> requestOriginal() {
    return requestOriginal;
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

  @Override
  public String toString() {
    return ToStringBuilder
      .of(RaptorResponse.class)
      .addObj("paths", paths)
      .addObj("requestOriginal", requestOriginal)
      .addObj("requestUsed", requestUsed)
      .toString();
  }
}
