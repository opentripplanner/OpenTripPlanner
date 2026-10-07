package org.opentripplanner.street.model.path;

import java.time.Instant;
import org.opentripplanner.street.model.edge.ExternalLegEdge;

/**
 * A leg consisting of a single {@link ExternalLegEdge}. The street module does not know what the
 * edge represents, so it is up to the caller to turn it into something meaningful.
 *
 * @param generalizedCost The generalized cost of traversing the edge
 */
public record ExternalEdgeLeg(
  ExternalLegEdge edge,
  Instant startTime,
  Instant endTime,
  int generalizedCost
) implements PathLeg {}
