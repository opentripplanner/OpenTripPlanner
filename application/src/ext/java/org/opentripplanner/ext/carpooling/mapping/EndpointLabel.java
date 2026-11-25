package org.opentripplanner.ext.carpooling.mapping;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Names one end of a carpool itinerary: with the transit stop at the transit end of an access or
 * egress ride ({@link #forStop}), or with the user's input location at the passenger's end
 * ({@link #forLocation}), never both. With neither ({@link #EMPTY}) the itinerary mapper names the
 * end after its street vertex.
 */
public record EndpointLabel(@Nullable StopLocation stop, @Nullable GenericLocation location) {
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
