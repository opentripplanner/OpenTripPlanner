package org.opentripplanner.transfer.dataimport.api;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.routing.api.request.RouteRequest;

/**
 * The configuration of the transfers created for flex routing, {@code transfers.flex} in the
 * build config.
 *
 * @param maxDuration the longest transfer between two stops
 * @param requests the routing requests the transfers are created with
 */
public record FlexTransferParameters(Duration maxDuration, List<RouteRequest> requests) {
  public static final FlexTransferParameters DEFAULT = new FlexTransferParameters(
    Duration.ofMinutes(30),
    List.of()
  );

  public FlexTransferParameters {
    Objects.requireNonNull(maxDuration);
    requests = List.copyOf(requests);
  }
}
