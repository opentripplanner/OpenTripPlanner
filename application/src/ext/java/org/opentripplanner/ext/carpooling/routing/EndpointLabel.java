package org.opentripplanner.ext.carpooling.routing;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Label data for one outermost endpoint of a carpool itinerary, consumed by the itinerary mapper
 * to name the corresponding {@code Place} in the API response.
 * <p>
 * The carpool flow has two independent label sources at each endpoint: a resolved transit stop
 * (for the transit-side end of an access/egress chain) or the user's {@code from}/{@code to}
 * input (for the passenger-side end). Exactly three states are valid, each produced by a single
 * factory:
 * <ul>
 *   <li>{@link #forStop(StopLocation)} — transit stop only</li>
 *   <li>{@link #forLocation(GenericLocation)} — user input location only</li>
 *   <li>{@link #EMPTY} — neither; the mapper falls back to vertex-derived naming</li>
 * </ul>
 * The canonical constructor rejects the fourth combination (both fields non-null), so callers
 * cannot accidentally build a label carrying both a stop and a user location.
 * <p>
 * The mapper enforces the precedence transit stop &gt; user input &gt; vertex; this record only
 * carries the two pre-resolved candidates.
 *
 * @param stop the transit-side stop, set on the transit end of an access/egress chain;
 *        {@code null} when the endpoint is not a transit stop.
 * @param location the passenger-side input location (the request's {@code from} or {@code to}),
 *        set on the user end of an access/egress chain or on both ends of a direct carpool
 *        itinerary; {@code null} when the endpoint is not the user's input.
 */
public record EndpointLabel(@Nullable StopLocation stop, @Nullable GenericLocation location) {
  /**
   * Use when the caller has neither a transit stop nor a user input location — the mapper will
   * name the endpoint from the underlying street vertex instead.
   */
  public static final EndpointLabel EMPTY = new EndpointLabel(null, null);

  public EndpointLabel {
    if (stop != null && location != null) {
      throw new IllegalArgumentException(
        "EndpointLabel carries either a stop or a user location, never both; " +
          "use the forStop/forLocation factories or EMPTY."
      );
    }
  }

  /** Use on the transit-side end of an access/egress chain. */
  public static EndpointLabel forStop(StopLocation stop) {
    return new EndpointLabel(Objects.requireNonNull(stop, "stop"), null);
  }

  /** Use on the passenger-side end (the request's {@code from} or {@code to} location). */
  public static EndpointLabel forLocation(GenericLocation location) {
    return new EndpointLabel(null, Objects.requireNonNull(location, "location"));
  }
}
